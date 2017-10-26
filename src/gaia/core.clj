(ns gaia.core
  (:require
   [taoensso.timbre :as log]
   [yaml.core :as yaml]
   [protograph.kafka :as kafka]
   [ophion.config :as config]
   [gaia.config :as gaia]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync]))

(defn boot-funnel
  [config]
  (let [kafka (:kafka config)
        funnel-config (assoc (:funnel config) :kafka kafka)]
    (log/info "funnel config" funnel-config)
    (funnel/funnel-connect funnel-config (:gaia config))))

(defn boot
  [config]
  (let [funnel (boot-funnel config)
        flow (sync/generate-sync funnel (:gaia config))
        events (sync/events-listener flow (:kafka config))]
    (sync/engage-sync! flow)
    flow))

(defn start
  []
  (let [config (config/read-config "config/gaia.clj")
        network (gaia/load-flow-config (get-in config [:flow :path]))]
    (boot
     (assoc config :gaia network))))

