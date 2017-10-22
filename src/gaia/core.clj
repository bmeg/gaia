(ns gaia.core
  (:require
   [protograph.kafka :as kafka]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync]))

(defn boot
  [config]
  (let [{:keys [commands processes variables agents]} (:gaia config)
        kafka (:kafka config)
        funnel (funnel/funnel-connect (:funnel config) commands)
        flow (flow/generate-flow commands processes)]
    {:funnel funnel
     :flow flow}))

(defn start
  [])

