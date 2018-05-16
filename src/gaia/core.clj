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

(defn boot
  [config]
  (let [commands (atom (:commands config))
        processes (atom {})
        flows (atom {})
        store (config/load-store (:store config))
        grandfather (store "")
        exec-config (assoc (:executor config) (:kafka config))
        executor (config/load-executor exec-config (store/protocol grandfather))]
    {:config config
     :commands commands
     :processes processes
     :flows flows
     :store store
     :executor executor}))

(defn state-processes
  [state root]
  (get @(:processes state) root))

(defn initiate-flow
  [{:keys [config executor store] :as state} root]
  (let [processes (state-processes state root)
        pointed (store (name root))
        flow (sync/generate-sync processes pointed)
        events (sync/events-listener executor flow (:kafka config))]
    (sync/engage-sync! executor flow)
    (swap! (:flows state) assoc root flow)
    state))

(defn commands-handler
  [state]
  (fn [request]
    (let [{:keys [commands] :as body} (read-json (:body request))]
      (log/info "commands request" body)
      (swap! (:commands state) merge body)
      (response
       {:commands (keys @(:commands state))}))))

(defn processes-handler
  [state]
  (fn [request]
    (let [{:keys [root processes] :as body} (read-json (:body request))
          root (keyword root)
          transformed (config/transform-processes (:commands state) processes)]
      (log/info "processes request" body)
      (swap! (:processes state) update root merge transformed)
      ;; TODO - add in code to merge new processes into running flow
      (response
       {:processes {root (keys (state-processes state root))}}))))

(defn initiate-handler
  [state]
  (fn [request]
    (let [{:keys [root] :as body} (read-json (:body request))
          root (keyword root)]
      (log/info "initiate request" body)
      (swap! (:processes state) update root merge body)
      (response
       {:processes {root (keys (get @(:processes state) root))}}))))

(defn status-handler
  [state]
  (fn [request]
    (let [{:keys [key] :as body} (read-json (:body request))]
      (log/info "status request" body)
      (response
       {:key key
        :status (get @(:status state) key)}))))

(defn expire-handler
  [state]
  (fn [request]
    (let [{:keys [key] :as body} (read-json (:body request))
          expired (sync/expire-key state key)]
      (log/info "expire request" body)
      (sync/trigger-election! state)
      (response
       {:expired expired}))))

(defn gaia-routes
  [state]
  [["/status" :status (status-handler state)]
   ["/expire" :expire (expire-handler state)]])

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-i" "--input INPUT" "input file or directory"]])

;; top level flow state
{:config
 {:kafka {}
  :mongo {}
  :store {}
  :executor {}
  :flow {}}
 :flows
 (atom
  {:$root-a
   {:processes {}
    :status (atom {})}})
 :commands (atom {})}

(defn start
  [options]
  (let [path (or (:config options) "resources/config/gaia.clj")
        config (config/load-config path)
        state (boot config)
        ;; flow (run config)
        routes (polaris/build-routes (gaia-routes state))
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
