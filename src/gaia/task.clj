(ns gaia.task)

(defprotocol Task
  (submit! [task process]))

;; TODO: make local docker execution task type
