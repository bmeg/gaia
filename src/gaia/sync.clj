(ns gaia.sync
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]
   [gaia.store :as store]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]))

(defn generate-sync
  [funnel {:keys [commands variables processes agents] :as config}]
  (let [flow (flow/generate-flow processes)]
    (merge
     config
     {:funnel funnel
      :flow flow
      :status (atom {})
      :next (agent {})})))

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
  [funnel prior tasks]
  (doseq [[key task] tasks]
    (if-not (get prior key)
      (funnel/submit-task! funnel task)))
  (merge tasks prior))

(defn elect-candidates!
  [flow funnel next status]
  (let [candidates (flow/find-candidates flow status)
        elect (process-map flow candidates)
        computing (apply merge (map compute-outputs (vals elect)))]
    (send next (partial send-tasks! funnel) elect)
    (merge computing status)))

(defn complete-key
  [status event]
  (assoc status (:key event) (:output event)))

(defn trigger-election!
  [{:keys [flow funnel next status]}]
  (swap! status (partial elect-candidates! flow funnel next)))

(defn process-complete!
  [{:keys [status] :as state} raw]
  (let [event (json/parse-string (.value raw) true)]
    (log/info "process complete!" event)
    (swap! status complete-key event)
    (trigger-election! state)))

(defn engage-sync!
  [{:keys [flow funnel status next] :as state}]
  (let [existing (store/existing-paths (:store funnel))]
    (swap! status merge existing)
    (trigger-election! state)))

(defn events-listener
  [state kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial process-complete! state)]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))

(defn expire-key
  [flow store key]
  (let [store (:store funnel)
        descendants (flow/find-descendants flow key)]
    (doseq [descendant descendants]
      (store/delete store descendant))
    (swap! (:status flow) (fn [status] (apply dissoc status descendants)))
    (log/info "deleted" descendants)
    descendants))
