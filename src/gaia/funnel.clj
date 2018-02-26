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
   [gaia.config :as config]))

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
        _ (log/info "body" body)
        response
        (http/post
         url
         {:body body
          :content-type :json})]
    response))

(defn render-output
  [root task-id {:keys [url path sizeBytes]}]
  (let [key (store/snip url root)]
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
  [path status task-id outputs]
  (let [rendered (render-outputs path task-id outputs)]
    rendered))

(defn declare-event!
  [producer message]
  (kafka/send-message
   producer
   "gaia-events"
   (json/generate-string message)))

(defn funnel-events-listener
  ([variables path status] (funnel-events-listener variables path status {}))
  ([variables path status kafka]
   (let [consumer (kafka/consumer (merge (:base kafka) (:consumer kafka)))
         producer (kafka/producer (merge (:base kafka) (:producer kafka)))

         listen
         (fn [funnel-event]
           (if-let [event (.value funnel-event)]
             (let [message (json/parse-string event true)]
               (log/info "funnel event" message)
               (if (= (:type message) "TASK_OUTPUTS")
                 (let [outputs (get-in message [:outputs :value])
                       applied (apply-outputs path status (:id message) outputs)]
                   (doseq [[key output] applied]
                     (log/info "funnel output" key output applied)
                     (declare-event!
                      producer
                      {:key key
                       :output output
                       :variable (get variables key)})))))))]
     (kafka/subscribe consumer ["funnel-events"])
     {:funnel-events (future (kafka/consume consumer listen))
      :consumer consumer})))

(defn funnel-config
  [{:keys [host path kafka store] :as config}
   {:keys [commands variables] :as context}]
  {:funnel config
   :commands commands
   :store store})

(defn funnel-connect
  [{:keys [host path zone kafka store] :as config}
   {:keys [commands variables] :as context}]
  (log/info "funnel connect" config)
  (let [tasks-url (str host "/v1/tasks")
        existing (store/existing-paths store)
        status (atom existing)
        prefix (str (store/protocol store) path)]
    (merge
     (funnel-config config context)
     {:listener (funnel-events-listener variables prefix status kafka)

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
         (str tasks-url "/" id ":cancel")))})))

(defn running-tasks
  [{:keys [list-tasks] :as funnel}]
  (let [tasks (list-tasks)]
    (log/info (first tasks))))

(defn funnel-path
  [funnel path]
  (let [store (:store funnel)
        prefix (store/protocol store)
        base (get-in funnel [:funnel :path])
        join (store/join-path [base path])]
    (str prefix join)))

(defn funnel-input
  [funnel inputs [key source]]
  (let [base {:name key
              :type "FILE"
              :path (get inputs (keyword key))}]
    (cond
      (string? source) (assoc base :url (funnel-path funnel source))
      (:contents source) (merge base source)
      (:content source) (assoc base :contents (:content source))
      (:type source) (merge base source)
      :else source)))

(defn missing-path
  [command key]
  (str "generated/" (name command) "/" (name key)))

(defn funnel-output
  [funnel command outputs [key path]]
  (let [source (or
                (get outputs (keyword key))
                (missing-path command key))
        base {:name key
              :type "FILE"
              :path path}]
    (cond
      (string? source) (assoc base :url (funnel-path funnel source))
      (:contents source) (merge base source)
      (:type source) (merge base source)
      :else source)))

(defn splice-vars
  [command vars]
  (let [vars (walk/stringify-keys vars)]
    (map #(template/evaluate-template % vars) command)))

(defn funnel-task
  [{:keys [commands] :as funnel}
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
       :inputs (map (partial funnel-input funnel (:inputs execute)) inputs)
       :outputs (map (partial funnel-output funnel key outputs) (:outputs execute))
       :executors [(assoc fun :workdir "/out")]})
    (log/error "no command named" command (keys commands))))

(defn submit-task!
  [funnel process]
  (try
    (let [task (funnel-task funnel process)
          task-id (:id ((:create-task funnel) task))]
      (log/info "funnel task" task-id task)
      (assoc task :id task-id))
    (catch Exception e
      (.printStackTrace e))))

(defn all-tasks
  [funnel processes]
  (mapv (partial funnel-task funnel) processes))

(def parse-args
  [["-c" "--config CONFIG" "path to config file"]
   ["-o" "--output OUTPUT" "path to output file"]])

(defn load-funnel-config
  [config]
  (let [store (config/load-store (:store config))
        funnel (assoc (:funnel config) :store store)]
    (funnel-config funnel (:gaia config))))

(defn -main
  [& args]
  (let [env (:options (cli/parse-opts args parse-args))
        path (or (:config env) "resources/config/gaia.clj")
        config (config/load-config path)
        funnel (load-funnel-config config)
        output (or (:output env) "funnel-tasks.json")
        tasks (all-tasks funnel (get-in config [:gaia :processes]))
        json (mapv json/generate-string tasks)
        out (string/join "\n" json)]
    (spit output out)))
