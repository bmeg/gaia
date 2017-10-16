(ns gaia.funnel
  (:require
   [cheshire.core :as json]
   [taoensso.timbre :as log]
   [clj-http.client :as http]
   [protograph.kafka :as kafka]))

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

(defn funnel-connect
  [{:keys [host] :as config} commands]
  (let [tasks-url (str host "/v1/tasks")]
    {:funnel config
     :commands commands

     :create-task
     (partial post-json tasks-url)

     :list-tasks
     (fn []
       (http/get tasks-url))

     :get-task
     (fn [id]
       (http/get
        (str tasks-url "/" id)))

     :cancel-task
     (fn [id]
       (http/post
        (str tasks-url "/" id ":cancel")))}))

(defn funnel-path
  [funnel path]
  (let [prefix (get-in funnel [:funnel :prefix] "file://")
        base (get-in funnel [:funnel :path] "tmp")]
    (str prefix base "/" path)))

(defn funnel-io
  [funnel [key source]]
  (let [base {:name key
              ;; :description (str key source)
              :type "FILE"
              :path (name key)}]
    (cond
      (string? source) (assoc base :url (funnel-path funnel source))
      (:contents source) (merge base source)
      (:type source) (merge base source)
      :else source)))

(defn funnel-task
  [{:keys [commands] :as funnel}
   {:keys [key inputs outputs command]}]
  (if-let [execute (get commands command)]
    {:name key
     ;; :description (str key inputs outputs command)
     :inputs (map (partial funnel-io funnel) inputs)
     :outputs (map (partial funnel-io funnel) outputs)
     :executors [execute]}
    (log/error "no command named" command)))

(defn submit-task
  [funnel process]
  (let [task (funnel-task funnel process)]
    (log/info "funnel task" task)
    ((:create-task funnel) task)))


;; EXAMPLE FUNNEL DOCUMENT
;; -----------------------
;; 
;; {
;;   "name": "Input file contents and output file",
;;   "description": "Demonstrates using the 'contents' field for inputs to create a file on the host system",
;;   "inputs": [
;;     {
;;       "name": "cat input",
;;       "description": "Input to md5sum. /tmp/in will be created on the host system.",
;;       "type": "FILE",
;;       "path": "/tmp/in",
;;       "contents": "Hello World\n"
;;     }
;;   ],
;;   "outputs": [
;;     {
;;       "name": "cat stdout",
;;       "description": "Stdout of cat is captures to /tmp/test_out on the host system.",
;;       "url": "file:///tmp/cat_output",
;;       "type": "FILE",
;;       "path": "/tmp/out"
;;     }
;;   ],
;;   "executors": [
;;     {
;;       "image_name": "alpine",
;;       "cmd": ["cat", "/tmp/in"],
;;       "stdout": "/tmp/out"
;;     }
;;   ]
;; }
