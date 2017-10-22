(ns gaia.store)

(defprotocol Store
  (absent? [store key])
  (computing? [store key])
  (present? [store key]))

(defprotocol Bus
  (put [bus topic message])
  (listen [bus topic fn]))

(defprotocol Executor
  (execute [executor key inputs outputs command])
  (status [executor task-id]))

