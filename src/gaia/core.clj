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
   [gaia.executor :as executor]
   [gaia.swift :as swift]
   [gaia.flow :as flow]
   [gaia.command :as command]
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

;; (defn boot-funnel
;;   [config store]
;;   (let [kafka (:kafka config)
;;         funnel-config (assoc (:funnel config) :kafka kafka :store store)]
;;     (log/info "funnel config" funnel-config)
;;     (funnel/funnel-connect funnel-config (:gaia config))))

(defn boot
  [config]
  (let [store (config/load-store (:store config))
        exec-config (assoc (:executor config) (:kafka config))
        executor (config/load-executor exec-config store)]
        ;; funnel (boot-funnel config store)
    ;; (sync/generate-sync funnel (:gaia config))
    {:store store
     :executor executor}))

(defn run
  [config]
  (let [flow (boot config)
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
      (log/info "expire request" body)
      (sync/trigger-election! flow)
      (response
       {:expired expired}))))

(defn commands-handler
  [flow]
  (fn [request]
    (let [{:keys [commands] :as body} (read-json (:body request))]
      (log/info "commands request" body)
      (swap! (:commands flow) merge body)
      (response
       {:commands (keys @(:commands flow))}))))

(defn processes-handler
  [flow]
  (fn [request]
    (let [{:keys [root processes] :as body} (read-json (:body request))
          root (keyword root)]
      (log/info "processes request" body)
      (swap! (:processes flow) update root merge body)
      (response
       {:processes {root (keys (get @(:processes flow) root))}}))))

(defn trigger-handler
  [flow]
  (fn [request]
    (let [{:keys [root] :as body} (read-json (:body request))
          root (keyword root)]
      (log/info "trigger request" body)
      (swap! (:processes flow) update root merge body)
      (response
       {:processes {root (keys (get @(:processes flow) root))}}))))

(defn gaia-routes
  [flow]
  [["/status" :status (status-handler flow)]
   ["/expire" :expire (expire-handler flow)]])

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-i" "--input INPUT" "input file or directory"]])

;; top level flow state
{:config
 {:kafka {}
  :mongo {}
  :store {}
  :task {}
  :flow {}}
 :status (atom {})
 :commands (atom {})}

(defn start
  [options]
  (let [path (or (:config options) "resources/config/gaia.clj")
        config (config/load-config path)
        flow (run config)
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
