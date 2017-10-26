(ns gaia.config
  (:require
   [taoensso.timbre :as log]
   [yaml.core :as yaml]))

(def config-keys
  [:variables
   :commands
   :processes
   :agents])

(defn load-flow-config
  [path]
  (into
   {}
   (map
    (fn [key]
      (try
        [key (yaml/parse-string (slurp (str path "." (name key) ".yaml")))]
        (catch Exception e (do (log/info "bad yaml" path key) [key {}]))))
    config-keys)))

