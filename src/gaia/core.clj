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
        flows (atom {})
        store (config/load-store (:store config))
        grandfather (store "")
        exec-config (assoc (:executor config) :kafka (:kafka config))
        prefix (str (store/protocol grandfather) (:path exec-config))
        executor (config/load-executor exec-config prefix)]
    {:config config
     :commands commands
     :flows flows
     :store store
     :executor executor}))

(defn initialize-flow!
  [{:keys [config commands executor store] :as state} root]
  (let [pointed (store (name root))
        flow (sync/generate-sync (:kafka config) root [] pointed)
        listener (sync/events-listener! flow executor commands root (:kafka config))]
    flow))

(defn find-flow!
  [{:keys [flows] :as state} root]
  (if-let [flow (get @flows root)]
    flow
    (let [flow (initialize-flow! state root)]
      (swap! flows assoc root flow)
      flow)))

(defn merge-processes!
  [{:keys [commands executor] :as state} root processes]
  (let [flow (find-flow! state (keyword root))]
    (sync/merge-processes! flow executor commands processes)
    state))

(defn load-processes!
  [state root path]
  (let [processes (config/parse-yaml path)]
    (merge-processes! state root processes)))

(defn trigger-flow!
  [{:keys [commands executor flows] :as state} root]
  (let [flow (find-flow! state (keyword root))]
    (sync/engage-sync! flow executor commands)
    state))

(defn halt-flow!
  [{:keys [executor tasks] :as state} root]
  (let [flow (find-flow! state (keyword root))]
    (sync/halt-flow! flow executor)
    state))

(defn expire-key!
  [{:keys [commands executor] :as state} root key]
  (let [flow (find-flow! state (keyword root))]
    (sync/expire-keys! flow executor commands [key])
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
          root (keyword root)]
      (log/info "processes request" body)
      (merge-processes! state root processes)
      (response
       {:processes {root (map :key processes)}}))))

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
    (trigger-flow! state key)))

(defn -main
  [& args]
  (let [env (:options (cli/parse-opts args parse-args))]
    (start env)))
