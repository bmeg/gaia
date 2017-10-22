(ns gaia.sync
  (:require
   [gaia.flow :as flow]))

(defprotocol Bus
  (put [bus topic message])
  (listen [bus topic fn]))

(defprotocol Executor
  (execute [executor key inputs outputs command])
  (status [executor task-id]))

(defn generate-sync
  [{:keys [commands variables processes agents]}
   {:keys [store bus executor]}]
  (let [flow (flow/generate-flow commands processes)]))
