(ns gaia.core
  (:require
   [taoensso.timbre :as log]
   [yaml.core :as yaml]
   [protograph.kafka :as kafka]
   [ophion.config :as config]
   [gaia.config :as gaia]
   [gaia.store :as store]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync]))

(defn boot-funnel
  [config store]
  (let [kafka (:kafka config)
        funnel-config (assoc (:funnel config) :kafka kafka :store store)]
    (log/info "funnel config" funnel-config)
    (funnel/funnel-connect funnel-config (:gaia config))))

(defn boot
  [config]
  (let [store (store/load-store (:store config))
        funnel (boot-funnel config store)
        flow (sync/generate-sync funnel (:gaia config))
        events (sync/events-listener flow (:kafka config))]
    (sync/engage-sync! flow)
    (assoc flow :store store)))

(defn load-config
  [path]
  (let [config (config/read-config path)
        network (gaia/load-flow-config (get-in config [:flow :path]))]
    (assoc config :gaia network)))

(defn start
  []
  (let [config (load-config "config/gaia.clj")]
    (boot config)))

