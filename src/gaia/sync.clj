(ns gaia.sync
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]
   [gaia.flow :as flow]
   [gaia.store :as store]
   [gaia.executor :as executor]))

(defn generate-sync
  [processes]
  (let [flow (flow/generate-flow processes)]
    {:flow flow
     :processes processes
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
  [executor commands prior tasks]
  (doseq [[key task] tasks]
    (if-not (get prior key)
      (executor/submit! executor commands task)))
  (merge tasks prior))

(defn elect-candidates!
  [flow executor next status]
  (let [commands (:commands flow)
        candidates (flow/find-candidates flow status)
        elect (process-map flow candidates)
        computing (apply merge (map compute-outputs (vals elect)))]
    (send next (partial send-tasks! executor commands) elect)
    (merge computing status)))

(defn complete-key
  [status event]
  (assoc status (:key event) (:output event)))

(defn trigger-election!
  [executor {:keys [flow next status]}]
  (swap! status (partial elect-candidates! flow executor next)))

(defn process-complete!
  [executor {:keys [status] :as state} raw]
  (let [event (json/parse-string (.value raw) true)]
    (log/info "process complete!" event)
    (swap! status complete-key event)
    (trigger-election! executor state)))

(defn engage-sync!
  [store executor {:keys [flow status next] :as state}]
  (let [existing (store/existing-paths store)]
    (swap! status merge existing)
    (trigger-election! state)))

(defn events-listener
  [executor state kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
        listen (partial process-complete! executor state)]
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
