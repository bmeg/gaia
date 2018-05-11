(ns gaia.executor)

(defprotocol Executor
  (submit! [executor process]))

;; TODO: make local docker execution task type
