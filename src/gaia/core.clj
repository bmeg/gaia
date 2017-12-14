(ns gaia.core
  (:require
   [taoensso.timbre :as log]
   [aleph.http :as http]
   [ring.middleware.resource :as resource]
   [ring.middleware.params :as params]
   [ring.middleware.keyword-params :as keyword]
   [cheshire.core :as json]
   [polaris.core :as polaris]
   [protograph.kafka :as kafka]
   [ophion.config :as config]
   [gaia.config :as gaia]
   [gaia.store :as store]
   [gaia.swift :as swift]
   [gaia.flow :as flow]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync])
  (:import
   [java.io InputStreamReader]))

(defn pp
  [clj]
  (with-out-str (clojure.pprint/pprint clj)))

(defn read-json
  [body]
  (json/parse-stream (InputStreamReader. body) keyword))

(defn response
  [body]
  {:status 200
   :headers {"content-type" "application/json"}
   :body (json/generate-string body)})

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

(defn status-handler
  [flow]
  (fn [request]
    (let [{:keys [key] :as body} (read-json (:body request))]
      (log/info "status request" body)
      (response
       {:key key
        :status (get @(:status flow) key)}))))

(defn expire-handler
  [flow]
  (fn [request]
    (let [{:keys [key] :as body} (read-json (:body request))
          implicated (flow/find-descendants (:flow flow) key)]
      (log/info "expire request" body)
      (swap! (:status flow) (fn [status] (apply dissoc status implicated)))
      (sync/trigger-election! flow)
      (response
       {:expired implicated}))))

(defn gaia-routes
  [flow]
  [["/status" :status (status-handler flow)]
   ["/expire" :expire (expire-handler flow)]])

(defn boot
  [config]
  (let [store (load-store (:store config))
        funnel (boot-funnel config store)
        flow (sync/generate-sync funnel (:gaia config))
        flow (assoc flow :store store)
        events (sync/events-listener flow (:kafka config))]
    (sync/engage-sync! flow)
    flow))

(defn start
  []
  (let [;; config (load-config "config/ohsu-swift.clj")
        ;; config (load-config "config/gaia.clj")
        config (load-config "config/home.clj")
        flow (boot config)
        routes (polaris/build-routes (gaia-routes flow))
        router (polaris/router routes)
        app (-> router
                (resource/wrap-resource "public")
                (keyword/wrap-keyword-params)
                (params/wrap-params))]
    (http/start-server app {:port 24442})))

(defn -main
  []
  (start))
