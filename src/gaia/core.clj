(ns gaia.core
  (:require
   [clojure.tools.cli :as cli]
   [taoensso.timbre :as log]
   [aleph.http :as http]
   [ring.middleware.resource :as resource]
   [ring.middleware.params :as params]
   [ring.middleware.keyword-params :as keyword]
   [cheshire.core :as json]
   [polaris.core :as polaris]
   [protograph.kafka :as kafka]
   [gaia.config :as config]
   [gaia.store :as store]
   [gaia.swift :as swift]
   [gaia.flow :as flow]
   [gaia.command :as command]
   [gaia.funnel :as funnel]
   [gaia.trigger :as trigger]
   [gaia.sync :as sync])
  (:import
   [java.io InputStreamReader]))

(defn read-json
  [body]
  (json/parse-stream (InputStreamReader. body) keyword))

(defn response
  [body]
  {:status 200
   :headers {"content-type" "application/json"}
   :body (json/generate-string body)})

(defn boot-funnel
  [config store]
  (let [kafka (:kafka config)
        funnel-config (assoc (:funnel config) :kafka kafka :store store)]
    (log/info "funnel config" funnel-config)
    (funnel/funnel-connect funnel-config (:gaia config))))

(defn boot
  [config]
  (let [store (config/load-store (:store config))
        funnel (boot-funnel config store)
        flow (sync/generate-sync funnel (:gaia config))
        ;; flow (assoc flow :store store)
        events (sync/events-listener flow (:kafka config))]
    (sync/engage-sync! flow)
    flow))

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
          expired (sync/expire-key flow key)]
          ;; implicated (flow/find-descendants (:flow flow) key)
      ;; (swap! (:status flow) (fn [status] (apply dissoc status implicated)))
      (log/info "expire request" body)
      (sync/trigger-election! flow)
      (response
       {:expired expired}))))

(defn gaia-routes
  [flow]
  [["/status" :status (status-handler flow)]
   ["/expire" :expire (expire-handler flow)]])

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-i" "--input INPUT" "input file or directory"]])

;; config (load-config "config/ohsu-swift.clj")
;; config (load-config "config/gaia.clj")

(defn start
  [options]
  (let [path (or (:config options) "resources/config/gaia.clj")
        config (config/load-config path)
        flow (boot config)
        routes (polaris/build-routes (gaia-routes flow))
        router (polaris/router routes)
        app (-> router
                (resource/wrap-resource "public")
                (keyword/wrap-keyword-params)
                (params/wrap-params))]
    (http/start-server app {:port 24442})))

(defn -main
  [& args]
  (let [env (:options (cli/parse-opts args parse-args))]
    (start env)))
