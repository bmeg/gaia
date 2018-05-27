(ns gaia.sync
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]
   [gaia.flow :as flow]
   [gaia.store :as store]
   [gaia.executor :as executor]))

(defn generate-sync
  [processes store]
  (let [flow (flow/generate-flow (vals processes))]
    {:flow flow
     :store store
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

(defn elect-candidates!
  [{:keys [flow store tasks] :as state} executor commands status]
  (let [candidates (flow/find-candidates flow (:data status))]
    (if (empty? candidates)
      (if (empty? (flow/missing-data flow status))
        (assoc status :state :complete)
        (assoc status :state :incomplete))
      (let [elect (process-map flow candidates)
            computing (apply merge (map compute-outputs (vals elect)))]
        (send tasks (partial send-tasks! executor store commands) elect)
        (update status :data merge computing)))))

(defn complete-key
  [status event]
  (assoc-in status [:data (:key event)] (:output event)))

(defn trigger-election!
  [{:keys [status] :as state} executor commands]
  (swap!
   status
   (partial elect-candidates! state executor @commands)))

(defn process-complete!
  [{:keys [status] :as state} executor commands root raw]
  (let [event (json/parse-string (.value raw) true)]
    (when (= (name root) (:root event))
      (log/info "process complete!" event)
      (swap! status complete-key event)
      (trigger-election! state executor commands))))

(defn initiate-sync
  [existing status]
  (-> status
      (update :data merge existing)
      (assoc :state :running)))

(defn engage-sync!
  [{:keys [flow store status] :as state} executor commands]
  (let [existing (store/existing-paths store)]
    (swap! status (partial initiate-sync existing))
    (trigger-election! state executor commands)))

(defn events-listener
  [state executor commands root kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial process-complete! state executor commands root)]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))

(defn dissoc-seq
  [m s]
  (apply dissoc m s))

(defn expunge-keys
  [state executor commands descendants status]
  (elect-candidates!
   state executor commands
   (update status :data dissoc-seq descendants)))

(defn expire-key!
  [{:keys [flow status] :as state} executor commands key]
  (let [descendants (flow/find-descendants flow key)]
    (swap! status (partial expunge-keys state executor @commands descendants))
    (log/info "expired" descendants)
    (trigger-election! state executor commands)
    descendants))
