(ns gaia.executor)

(defprotocol Executor
  (submit! [executor commands process]))

;; TODO: make local docker execution task type
