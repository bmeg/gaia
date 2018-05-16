{:kafka
 {:base
  {:host "localhost"
   :port "9092"}}

 :mongo
 {:host "127.0.0.1"
  :port 27017
  :database "test"}

 :executor
 {:target "funnel"
  :host "http://localhost:19191"
  :path "/Users/spanglry/Code/gaia/resources/test/data"
  :zone "gaia"}

 :store
 {:type :file
  :root "/Users/spanglry/Code/gaia/resources/test/data"}

 :flow
 {:path "resources/test/triangle/triangle"}}
