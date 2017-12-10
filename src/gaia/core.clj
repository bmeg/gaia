(ns gaia.core
  (:require
   [taoensso.timbre :as log]
   [aleph.http :as http]
   [protograph.kafka :as kafka]
   [ophion.config :as config]
   [gaia.config :as gaia]
   [gaia.store :as store]
   [gaia.swift :as swift]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync]))

(defn pp
  [clj]
  (with-out-str (clojure.pprint/pprint clj)))

(defn load-config
  [path]
  (let [config (config/read-config path)
        network (gaia/load-flow-config (get-in config [:flow :path]))]
    (log/info "config" (pp network))
    (assoc config :gaia network)))

(defn load-store
  [config]
  (condp = (keyword (:type config))
    :file (store/load-file-store config)
    :swift (swift/load-swift-store config)
    (store/load-file-store config)))

(defn boot-funnel
  [config store]
  (let [kafka (:kafka config)
        funnel-config (assoc (:funnel config) :kafka kafka :store store)]
    (log/info "funnel config" funnel-config)
    (funnel/funnel-connect funnel-config (:gaia config))))

(defn boot
  [config]
  (let [store (load-store (:store config))
        funnel (boot-funnel config store)
        flow (sync/generate-sync funnel (:gaia config))
        events (sync/events-listener flow (:kafka config))]
    (sync/engage-sync! flow)
    (assoc flow :store store)))

(defn app
  [request]
  {:status 200
   :body "yellow"})

;; (defn start
;;   []
;;   (let [config (load-config "config/ohsu-swift.clj")]
;;     (boot config)))

(defn start
  []
  (let [config (load-config "config/home.clj")]
    (boot config)))

;; (defn start
;;   []
;;   (let [config (load-config "config/gaia.clj")]
;;     (boot config)))

(defn -main
  []
  (start)
  (http/start-server app {:port 24442}))
