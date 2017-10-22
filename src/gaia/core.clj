(ns gaia.core
  (:require
   [taoensso.timbre :as log]
   [yaml.core :as yaml]
   [protograph.kafka :as kafka]
   [ophion.config :as config]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync]))

(defn boot
  [config]
  (let [kafka (:kafka config)
        funnel-config (assoc (:funnel config) :kafka kafka)
        _ (log/info "funnel config" funnel-config)
        funnel (funnel/funnel-connect funnel-config (:gaia config))
        flow (sync/generate-sync funnel (:gaia config))
        events (sync/events-listener flow kafka)]
    (sync/engage-sync! flow)))

(defn start
  []
  (let [config (config/read-config "config/gaia.clj")
        network (yaml/parse-string (slurp "resources/config/bmeg.yaml"))]
    (boot
     (assoc config :gaia network))))

