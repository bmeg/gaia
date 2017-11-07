(ns gaia.sync
  (:require
   [taoensso.timbre :as log]
   [protograph.kafka :as kafka]
   [gaia.store :as store]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]))

(defn generate-sync
  [funnel {:keys [commands variables processes agents] :as config}]
  (let [flow (flow/generate-flow commands processes)
        listen ()]
    (merge
     config
     {:funnel funnel
      :flow flow})))

(defn find-process
  [flow key]
  (get-in flow [:process (name key) :node]))

(defn sync-process
  [{:keys [flow funnel]} key]
  (log/info "run" key)
  (let [process (find-process flow key)
        result (funnel/submit-task funnel process)]
    result))

(defn sync-step
  [{:keys [flow funnel] :as state}]
  (let [candidates (flow/find-candidates flow @(:status funnel))
        outcome (mapv (partial sync-process state) candidates)]
    outcome))

(defn engage-sync!
  [{:keys [flow funnel] :as state}]
  (let [existing (store/existing-paths (:store funnel))
        status (swap! (:status funnel) merge existing)]
    (if (flow/flow-complete? flow status)
      {:status status :complete? true}
      (sync-step state))))

(defn events-listener
  [state kafka]
  (let [consumer (kafka/consumer (merge (:base kafka) (:consuemr kafka)))
        listen (fn [event]
                 (log/info "computation complete!" event)
                 (engage-sync! state))]
    (kafka/subscribe consumer ["gaia-events"])
    {:gaia-events (future (kafka/consume consumer listen))
     :consumer consumer}))
