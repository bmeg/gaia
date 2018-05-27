(ns gaia.executor
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [protograph.kafka :as kafka]))

(defprotocol Executor
  (submit! [executor store commands process]))

(defn declare-event!
  [producer message]
  (log/info "declare event" message)
  (kafka/send-message
   producer
   "gaia-events"
   (json/generate-string message)))

;; TODO: make local docker execution task type
