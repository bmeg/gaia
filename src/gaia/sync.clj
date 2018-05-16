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
     :processes (vals processes)
     :status (atom {})
     :next (agent {})}))

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
  (doseq [[key task] tasks]
    (if-not (get prior key)
      (executor/submit! executor store commands task)))
  (merge tasks prior))

(defn elect-candidates!
  [{:keys [flow store next] :as state} executor commands status]
  (let [candidates (flow/find-candidates flow status)
        elect (process-map flow candidates)
        computing (apply merge (map compute-outputs (vals elect)))]
    (send next (partial send-tasks! executor store @commands) elect)
    (merge computing status)))

(defn complete-key
  [status event]
  (assoc status (:key event) (:output event)))

(defn trigger-election!
  [{:keys [status] :as state} executor commands]
  (swap! status (partial elect-candidates! state executor commands)))

(defn process-complete!
  [{:keys [status] :as state} executor commands root raw]
  (let [event (json/parse-string (.value raw) true)]
    (when (= (name root) (:root event))
      (log/info "process complete!" event)
      (swap! status complete-key event)
      (trigger-election! state executor commands))))

(defn engage-sync!
  [{:keys [flow store status next] :as state} executor commands]
  (let [existing (store/existing-paths store)]
    (swap! status merge existing)
    (trigger-election! state executor commands)))

(defn events-listener
  [state executor commands root kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial process-complete! state executor commands root)]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))

(defn expire-key
  [flow key]
  (let [descendants (flow/find-descendants (:flow flow) key)]
        ;; store (get-in flow [:funnel :store])
    ;; (doseq [descendant descendants]
    ;;   (try
    ;;     (store/delete store descendant)
    ;;     (catch Exception e (log/info (.getMessage e)))))
    (swap!
     (:status flow)
     (fn [status]
       (apply dissoc status descendants)))
    (log/info "expired" descendants)
    descendants))
