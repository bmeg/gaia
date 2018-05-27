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
        exec-config (assoc (:executor config) :kafka (:kafka config))
        prefix (str (store/protocol grandfather) (:path exec-config))
        executor (config/load-executor exec-config prefix)]
    {:config config
     :commands commands
     :processes processes
     :flows flows
     :store store
     :executor executor}))

(defn merge-processes!
  [state root processes]
  (let [transformed (config/transform-processes @(:commands state) processes)]
    (swap! (:processes state) update root merge transformed)
    state))

(defn load-processes!
  [state root path]
  (let [processes (config/parse-yaml path)]
    (merge-processes! state root processes)))

(defn state-processes
  [state root]
  (get @(:processes state) root))

(defn state-flow
  [{:keys [flows]} flow]
  (get @flows (keyword flow)))

(defn initiate-flow!
  [{:keys [config commands executor store] :as state} root]
  (let [processes (state-processes state root)
        pointed (store (name root))
        flow (sync/generate-sync (:kafka config) root processes pointed)
        listener (sync/events-listener! flow executor commands root (:kafka config))]
    (sync/engage-sync! flow executor commands)
    (swap! (:flows state) assoc root flow)
    state))

(defn trigger-flow!
  [{:keys [commands executor flows] :as state} root]
  (let [flow (get @flows (keyword root))]
    (sync/engage-sync! flow executor commands)))

(defn halt-flow!
  [{:keys [executor flows tasks] :as state} root]
  (let [flow (get @flows (keyword root))]
    (sync/halt-flow! flow executor)))

(defn expire-key!
  [{:keys [commands executor flows] :as state} root key]
  (let [flow (get @flows (keyword root))]
    (sync/expire-keys! flow executor commands [key])))

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
          expired (expire-key! state key)]
      (log/info "expire request" body)
      (response
       {:expired expired}))))

(defn gaia-routes
  [state]
  [["/status" :status (status-handler state)]
   ["/expire" :expire (expire-handler state)]])

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-i" "--input INPUT" "input file or directory"]])

(defn start
  [options]
  (let [path (or (:config options) "resources/config/gaia.clj")
        config (config/load-config path)
        state (boot config)
        routes (polaris/build-routes (gaia-routes state))
        router (polaris/router routes)
        app (-> router
                (resource/wrap-resource "public")
                (keyword/wrap-keyword-params)
                (params/wrap-params))]
    (http/start-server app {:port 24442})))

(defn load
  [key config-path process-path]
  (let [config (config/load-config config-path)
        state (boot config)
        state (load-processes! state key process-path)]
    (initiate-flow! state key)))

(defn -main
  [& args]
  (let [env (:options (cli/parse-opts args parse-args))]
    (start env)))
