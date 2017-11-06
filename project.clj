(defproject gaia "0.0.2"
  :description "regenerating computational dependency network"
  :url "http://github.com/bmeg/gaia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main gaia.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.8.0"]
                 [clj-http "3.7.0"]
                 [ophion "0.0.5"]
                 [org.javaswift/joss "0.9.17"]])
