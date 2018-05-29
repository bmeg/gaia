(ns gaia.sync
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]
   [gaia.flow :as flow]
   [gaia.command :as command]
   [gaia.store :as store]
   [gaia.executor :as executor]))

(defn generate-sync
  [{:keys [kafka] :as config} root processes store]
  (let [flow (flow/generate-flow (vals processes))]
    {:root root
     :flow (atom flow)
     :store store
     :events (kafka/producer (merge (:base kafka) (:producer kafka)))
     :status
     (atom
      {:state :initialized
       :data {}})
     :tasks (agent {})}))

(defn send-tasks!
  [executor store commands prior tasks]
  (let [relevant (remove (comp (partial get prior) first) tasks)
        submit! (partial executor/submit! executor store commands)
        triggered (into
                   {}
                   (map
                    (fn [[key task]]
                      [key (submit! task)])
                    relevant))]
    (merge triggered prior)))

(defn reset-tasks!
  [{:keys [tasks] :as state} status reset]
  (send tasks (partial apply dissoc) reset))

(defn compute-outputs
  [process]
  (into
   {}
   (map
    (fn [k]
      [k {:state :computing}])
    (vals (:outputs process)))))

(defn elect-candidates!
  [{:keys [store tasks] :as state} flow executor commands status]
  (let [candidates (mapv identity (flow/find-candidates flow (:data status)))]
    (log/info "candidates" candidates)
    (if (empty? candidates)
      (let [missing (flow/missing-data flow (:data status))]
        (log/info "empty candidates - missing" missing)
        (if (empty? missing)
          (assoc status :state :complete)
          (assoc status :state :incomplete)))
      (let [elect (flow/process-map flow candidates)
            computing (apply merge (map compute-outputs (vals elect)))]
        (send tasks (partial send-tasks! executor store commands) elect)
        (-> status
            (update :data merge computing)
            (assoc :state :running))))))

(defn complete-key
  [event status]
  (assoc-in status [:data (:key event)] (:output event)))

(defn process-state!
  [{:keys [tasks]} event]
  (send
   tasks
   (fn [ts {:keys [id state]}]
     (if-let [found (first (filter #(= id (:id (last %))) ts))]
       (let [[key task] found]
         (assoc-in ts [key :state] (keyword (string/lower-case state))))))
   event))

(defn data-complete!
  [{:keys [flow status events] :as state} executor commands root event]
  (if (= :halted (:state @status))
    (swap! status update :data dissoc (:key event))
    (do
      (swap!
       status
       (comp
        (partial elect-candidates! state @flow executor @commands)
        (partial complete-key event)))
      (condp = (:state @status)

        :complete
        (executor/declare-event!
         events
         {:event "flow-complete"
          :root root})

        :incomplete
        (executor/declare-event!
         events
         {:event "flow-incomplete"
          :root root})

        (log/info "FLOW CONTINUES" root)))))

(defn executor-events!
  [{:keys [status events] :as state} executor commands root raw]
  (let [event (json/parse-string (.value raw) true)]
    (log/info "GAIA EVENT" event)
    (condp = (:event event)

      "process-state"
      (process-state! state event)

      "data-complete"
      (when (= (:root event) (name root))
        (data-complete! state executor commands root event))

      (log/info "other executor event" event))))

(defn find-existing
  [store status]
  (let [existing (store/existing-paths store)]
    (update status :data merge existing)))

(defn events-listener!
  [state executor commands root kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial executor-events! state executor commands root)]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))

(defn initialize-flow!
  [root store executor kafka commands]
  (let [flow (generate-sync kafka root [] store)
        listener (events-listener! flow executor commands root kafka)]
    (swap! (:status flow) (partial find-existing store))
    flow))

(defn trigger-flow!
  [{:keys [flow store status] :as state} executor commands]
  (swap!
   status
   (comp
    (partial elect-candidates! state @flow executor @commands)
    (partial find-existing store))))

(defn dissoc-seq
  [m s]
  (apply dissoc m s))

(defn expunge-keys
  [descendants status]
  (update status :data dissoc-seq descendants))

(defn expire-keys!
  [{:keys [flow status tasks] :as state} executor commands expiring]
  (let [now (deref flow)
        {:keys [data process] :as down} (flow/find-descendants now expiring)]
    (send tasks dissoc-seq process)
    (swap!
     status
     (comp
      (partial elect-candidates! state now executor @commands)
      (partial expunge-keys data)))
    (log/info "expired" down)
    down))

(defn running-task?
  [{:keys [state] :as task}]
  (or
   (= :initializing state)
   (= :running state)))

(defn executor-cancel!
  [executor tasks outstanding]
  (let [potential (select-keys tasks outstanding)
        canceling (filter running-task? (vals potential))
        expunge (mapv :name canceling)]
    (log/info "canceling tasks" expunge)
    (doseq [cancel canceling]
      (executor/cancel! executor (:id cancel)))
    (apply dissoc tasks expunge)))

(defn cancel-tasks!
  [tasks executor canceling]
  (send tasks (partial executor-cancel! executor) canceling))

(defn halt-flow!
  [{:keys [root flow tasks status events] :as state} executor]
  (let [halting (flow/process-keys @flow)]
    (swap! status assoc :state :halted)
    (cancel-tasks! tasks executor halting)
    (executor/declare-event!
     events
     {:event "flow-halted"
      :root root})))

(defn merge-processes!
  [{:keys [flow status tasks] :as state} executor commands processes]
  (let [transform (command/transform-processes @commands processes)]
    (cancel-tasks! tasks executor (keys transform))
    (swap! flow #(flow/add-nodes % (vals transform)))
    (expire-keys! state executor commands (keys transform))))
