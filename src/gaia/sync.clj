(ns gaia.sync
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]
   [gaia.flow :as flow]
   [gaia.store :as store]
   [gaia.executor :as executor]))

(defn generate-sync
  [{:keys [kafka] :as config} processes store]
  (let [flow (flow/generate-flow (vals processes))]
    {:flow flow
     :store store
     :events (kafka/producer (merge (:base kafka) (:producer kafka)))
     :status
     (atom
      {:state :initialized
       :data {}})
     :tasks (agent {})}))

(defn find-process
  [flow key]
  (get-in flow [:process (name key) :node]))

(defn compute-outputs
  [process]
  (into
   {}
   (map
    (fn [k]
      [k {:state :computing}])
    (vals (:outputs process)))))

(defn process-map
  [flow keys]
  (reduce
   (fn [m key]
     (assoc m key (find-process flow key)))
   {} keys))

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

(defn elect-candidates!
  [{:keys [flow store tasks] :as state} executor commands status]
  (let [candidates (mapv identity (flow/find-candidates flow (:data status)))]
    (log/info "candidates" candidates)
    (if (empty? candidates)
      (let [missing (flow/missing-data flow (:data status))]
        (log/info "empty candidates, missing" missing)
        (if (empty? missing)
          (assoc status :state :complete)
          (assoc status :state :incomplete)))
      (let [elect (process-map flow candidates)
            computing (apply merge (map compute-outputs (vals elect)))]
        (send tasks (partial send-tasks! executor store commands) elect)
        (-> status
            (update :data merge computing)
            (assoc :state :running))))))

(defn complete-key
  [event status]
  (assoc-in status [:data (:key event)] (:output event)))

(defn process-complete!
  [state event])

(defn data-complete!
  [{:keys [status events] :as state} executor commands root event]
  (swap!
   status
   (comp
    (partial elect-candidates! state executor @commands)
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

    (log/info "FLOW CONTINUES" root)))

(defn executor-events!
  [{:keys [status events] :as state} executor commands root raw]
  (let [event (json/parse-string (.value raw) true)]
    (log/info "GAIA EVENT" event)
    (when (= (:root event) (name root))
      (condp = (:event event)
        "process-complete" (process-complete! state event)
        "data-complete" (data-complete! state executor commands root event)
        (log/info "other executor event" event)))))

(defn initiate-sync
  [store status]
  (let [existing (store/existing-paths store)]
    (update status :data merge existing)))

(defn engage-sync!
  [{:keys [flow store status] :as state} executor commands]
  (swap!
   status
   (comp
    (partial elect-candidates! state executor @commands)
    (partial initiate-sync store))))

(defn data-listener
  [state executor commands root kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial executor-events! state executor commands root)]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))

(defn dissoc-seq
  [m s]
  (apply dissoc m s))

(defn expunge-keys
  [descendants status]
  (update status :data dissoc-seq descendants))

(defn expire-key!
  [{:keys [flow status tasks] :as state} executor commands key]
  (let [{:keys [data process] :as down} (flow/find-descendants flow key)]
    (send tasks dissoc-seq process)
    (swap!
     status
     (comp
      (partial elect-candidates! state executor @commands)
      (partial expunge-keys data)))
    (log/info "expired" down)
    down))
