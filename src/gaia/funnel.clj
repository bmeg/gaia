(ns gaia.funnel
  (:require
   [clojure.walk :as walk]
   [clojure.string :as string]
   [clojure.tools.cli :as cli]
   [cheshire.core :as json]
   [taoensso.timbre :as log]
   [clj-http.client :as http]
   [protograph.kafka :as kafka]
   [protograph.template :as template]
   [gaia.store :as store]
   [gaia.executor :as executor]))

(defn parse-body
  [response]
  (let [body (:body response)
        parsed (json/parse-string body true)]
    (if (:error parsed)
      (log/info "error getting json for" parsed)
      parsed)))

(defn get-json
  [url]
  (let [response (http/get url {:throw-exceptions false})]
    (parse-body response)))

(defn post-json
  [url task]
  (log/info "post:" task)
  (let [body (json/generate-string task)
        response
        (http/post
         url
         {:body body
          :content-type :json})]
    response))

(defn render-output
  [prefix task-id {:keys [url path sizeBytes]}]
  (let [key (store/snip url prefix)]
    [key
     {:url url
      :path path
      :size sizeBytes
      :state :complete
      :source task-id}]))

(defn render-outputs
  [path task-id outputs]
  (into
   {}
   (map
    (partial render-output path task-id)
    outputs)))

(defn apply-outputs
  [path task-id outputs]
  (let [rendered (render-outputs path task-id outputs)]
    rendered))

(defn extract-root
  [outputs prefix]
  (log/info "EXTRACT ROOT" (first outputs) prefix)
  (let [output (first outputs)
        base (store/snip (:url output) prefix)
        parts (string/split base #"/")]
    (log/info "PARTS" parts)
    (first parts)))

(defn task-state!
  [producer prefix message]
  (try
    (executor/declare-event!
     producer
     {:event "process-state"
      :id (:id message)
      :state (:state message)})
    (catch Exception e (.printStackTrace e))))

(defn task-outputs!
  [producer prefix message]
  (try
    (let [outputs (get-in message [:outputs :value])
          root (extract-root outputs prefix)
          full (str prefix root "/")
          applied (apply-outputs full (:id message) outputs)]
      (doseq [[key output] applied]
        (log/info "funnel output" key output applied)
        (executor/declare-event!
         producer
         {:event "data-complete"
          :root root
          :key key
          :output output})))
    (catch Exception e (.printStackTrace e))))

(defn funnel-events
  ([prefix] (funnel-events prefix {}))
  ([prefix kafka]
   (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
         producer (kafka/producer (merge (:base kafka) (:producer kafka)))

         listen
         (fn [funnel-event]
           (if-let [event (.value funnel-event)]
             (let [message (json/parse-string event true)]
               (condp = (:type message)
                 "TASK_STATE" (task-state! producer prefix message)
                 "TASK_OUTPUTS" (task-outputs! producer prefix message)
                 (log/info "unused funnel event" message)))))]
     (kafka/subscribe consumer ["funnel"])
     {:funnel-events (future (kafka/consume consumer listen))
      :consumer consumer})))

(defn funnel-connect
  [{:keys [host path zone kafka] :as config} prefix]
  (log/info "funnel connect" config)
  (let [tasks-url (str host "/v1/tasks")]
    {:funnel config
     :listener (funnel-events prefix kafka)

     ;; api functions
     :create-task
     (comp parse-body (partial post-json tasks-url))

     :list-tasks
     (fn []
       (get-json tasks-url))

     :get-task
     (fn [id]
       (get-json (str tasks-url "/" id)))

     :cancel-task
     (fn [id]
       (http/post
        (str tasks-url "/" id ":cancel")))}))

(defn running-tasks
  [{:keys [list-tasks] :as funnel}]
  (let [tasks (list-tasks)]
    (log/info (first tasks))))

(defn funnel-input
  [funnel store inputs [key source]]
  (let [base {:name key
              :type "FILE"
              :path (get inputs (keyword key))}]
    (cond
      (string? source) (assoc base :url (store/key->url store source))
      (:contents source) (merge base source)
      (:content source) (assoc base :contents (:content source))
      (:type source) (merge base source)
      :else source)))

(defn missing-path
  [command key]
  (str "generated/" (name command) "/" (name key)))

(defn funnel-output
  [funnel store command outputs [key path]]
  (let [source (or
                (get outputs (keyword key))
                (missing-path command key))
        base {:name key
              :type "FILE"
              :path path}]
    (cond
      (string? source) (assoc base :url (store/key->url store source))
      (:contents source) (merge base source)
      (:type source) (merge base source)
      :else source)))

(defn splice-vars
  [command vars]
  (let [vars (walk/stringify-keys vars)]
    (map #(template/evaluate-template % vars) command)))

(defn funnel-task
  [funnel store commands
   {:keys [key vars inputs outputs command]}]
  (if-let [raw (get commands (keyword command))]
    (let [all-vars (merge (:vars raw) vars)
          execute (update raw :command splice-vars all-vars)
          execute (update execute :command (partial remove empty?))
          fun (dissoc execute :key :vars :inputs :outputs :repo)]
      {:name key
       :resources {:cpuCores 1 :zones [(or (get-in funnel [:funnel :zone]) "gaia")]}
       :tags {"gaia" "true"}
       :volumes ["/in" "/out"]
       :inputs (map (partial funnel-input funnel store (:inputs execute)) inputs)
       :outputs (map (partial funnel-output funnel store key outputs) (:outputs execute))
       :executors [(assoc fun :workdir "/out")]})
    (log/error "no command named" command (keys commands))))

(defn submit-task!
  [funnel store commands process]
  (try
    (let [task (funnel-task funnel store commands process)
          task-id (:id ((:create-task funnel) task))]
      (log/info "funnel task" task-id task)
      (assoc task :id task-id))
    (catch Exception e
      (.printStackTrace e))))

(defn cancel-task!
  [funnel id]
  ((:cancel-task funnel) id))

(deftype FunnelExecutor [funnel]
  executor/Executor
  (submit!
    [executor store commands process]
    (submit-task! funnel store commands process))
  (cancel!
    [executor id]
    (cancel-task! funnel id)))

(defn load-funnel-executor
  [config prefix]
  (log/info "PREFIX" prefix)
  (let [funnel (funnel-connect config prefix)]
    (FunnelExecutor. funnel)))

(defn all-tasks
  [funnel store commands processes]
  (mapv (partial funnel-task funnel store commands) processes))

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-o" "--output OUTPUT" "path to output file"]])

;; (defn -main
;;   [& args]
;;   (let [env (:options (cli/parse-opts args parse-args))
;;         path (or (:config env) "resources/config/gaia.clj")
;;         config (config/load-config path)
;;         store (config/load-store (:store config))
;;         funnel (funnel-config (:funnel config) store (get-in config [:gaia :commands]))
;;         output (or (:output env) "funnel-tasks.json")
;;         tasks (all-tasks funnel (get-in config [:gaia :processes]))
;;         json (mapv json/generate-string tasks)
;;         out (string/join "\n" json)]
;;     (spit output out)))
