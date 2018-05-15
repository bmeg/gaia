(ns gaia.executor)

(defprotocol Executor
  (submit! [executor store commands process]))

;; TODO: make local docker execution task type
