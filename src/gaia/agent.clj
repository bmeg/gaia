(ns gaia.agent)

(defn generate-listener
  [agent]
  (fn [state raw]
    (let [event (json/parse-string (.value raw) true)])))

(defn agent-listener
  [state raw]
  (let [event (json/parse-string (.value raw) true)]))

(defn generate-listeners
  [agents])
