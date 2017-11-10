(ns gaia.config
  (:require
   [taoensso.timbre :as log]
   [yaml.core :as yaml]))

(def config-keys
  [:variables
   :commands
   :processes
   :agents])

(defn parse-yaml
  [path]
  (yaml/parse-string (slurp path)))

(defn load-flow-config
  [path]
  (into
   {}
   (map
    (fn [key]
      (try
        [key (parse-yaml (str path "." (name key) ".yaml"))]
        (catch Exception e (do (log/info "bad yaml" path key) [key {}]))))
    config-keys)))

