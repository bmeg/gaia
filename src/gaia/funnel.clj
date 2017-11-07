(ns gaia.funnel
  (:require
   [cheshire.core :as json]
   [taoensso.timbre :as log]
   [clj-http.client :as http]
   [protograph.kafka :as kafka]
   [protograph.template :as template]
   [gaia.store :as store]))

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
  (let [key (store/snip path root)]
    [key
     {:url url
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

(defn apply-outputs!
  [path status task-id outputs]
  (let [rendered (render-outputs path task-id outputs)]
    (swap!
     status
     (fn [status]
       (merge status rendered)))
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
                       applied (apply-outputs! path status (:id message) outputs)]
                   (log/info "funnel event outputs" outputs applied)
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

(defn funnel-connect
  [{:keys [host path kafka store] :as config}
   {:keys [commands variables] :as context}]
  (log/info "funnel connect" config)
  (let [tasks-url (str host "/v1/tasks")
        existing (store/existing-paths store)
        status (atom existing)]
    {:funnel config
     :commands commands
     :store store

     :status status
     :listener (funnel-events-listener variables path status kafka)

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

(defn funnel-path
  [funnel path]
  (let [prefix (get-in funnel [:funnel :prefix] "file://")
        base (get-in funnel [:funnel :path] "tmp")]
    (str prefix base "/" path)))

(defn funnel-input
  [funnel inputs [key source]]
  (let [base {:name key
              ;; :description (str key source)
              :type "FILE"
              :path (get inputs (keyword key))}]
    (cond
      (string? source) (assoc base :url (funnel-path funnel source))
      (:contents source) (merge base source)
      (:content source) (assoc base :contents (:content source))
      (:type source) (merge base source)
      :else source)))

(defn funnel-output
  [funnel outputs [key source]]
  (let [base {:name key
              ;; :description (str key source)
              :type "FILE"
              :path (get outputs (keyword key))}]
    (cond
      (string? source) (assoc base :url (funnel-path funnel source))
      (:contents source) (merge base source)
      (:type source) (merge base source)
      :else source)))

(defn splice-vars
  [command vars]
  (map #(template/evaluate-template % vars) command))

(defn funnel-task
  [{:keys [commands] :as funnel}
   {:keys [key vars inputs outputs command]}]
  (if-let [raw (get commands (keyword command))]
    (let [execute (update raw :cmd splice-vars vars)]
      {:name key
       ;; :description (str key inputs outputs command)
       :inputs (map (partial funnel-input funnel (:inputs execute)) inputs)
       :outputs (map (partial funnel-output funnel (:outputs execute)) outputs)
       :executors [(dissoc execute :inputs :outputs :repo)]})
    (log/error "no command named" command)))

(defn submit-task
  [funnel process]
  (let [task (funnel-task funnel process)
        task-id (:id ((:create-task funnel) task))
        computing (into
                   {}
                   (map
                    (fn [k]
                      [k {:source task-id :state :computing}])
                    (vals (:outputs process))))]
    (swap!
     (:status funnel)
     (fn [status]
       (merge computing status)))
    (log/info "funnel task" task-id task)))

(defn funnel-status
  [funnel key]
  (get-in @(:status funnel) [key :url]))

(defn pull-data
  [funnel inputs]
  (into
   {}
   (map
    (fn [[arg key]]
      [arg (funnel-status funnel key)])
    inputs)))

(defn stuff-data
  [data outputs]
  (into
   {}
   (map
    (fn [[out key]]
      [key (funnel-path data out)])
    outputs)))


























;; FUNNEL STORE ???????? simplicity may be better

;; (deftype FunnelStore [state]
;;   store/Store
;;   (absent? [store key]
;;     (empty? (get state key)))
;;   (computing? [store key]
;;     (= (get state key) :computing))
;;   (present? [store key]
;;     (= (get state key) :present)))

;; (deftype FunnelStore [state]
;;   store/Store
;;   (absent? [store key]
;;     (empty? (get state key)))
;;   (computing? [store key]
;;     (if-let [source (get state key)]
;;       (let [status ((:get-task funnel) source)]
;;         (not= (:state status) "COMPLETE"))))
;;   (present? [store key]
;;     (if-let [source (get state key)]
;;       (let [status ((:get-task funnel) source)]
;;         (= (:state status) "COMPLETE")))))
