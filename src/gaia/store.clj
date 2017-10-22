(ns gaia.store)

(defprotocol Store
  (absent? [store key])
  (computing? [store key])
  (present? [store key]))

