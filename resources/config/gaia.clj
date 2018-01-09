{:kafka
 {:base
  {:host "localhost"
   :port "9092"}}

 :mongo
 {:host "127.0.0.1"
  :port 27017
  :database "test"}

 :funnel
 {:host "http://localhost:19191"
  :path "/Users/spanglry/Data/funnel"}

 :store
 {:type :file
  :root "/Users/spanglry/Data/funnel"}

 :flow
 {:path "../biostream/bmeg-etl/bmeg"}}
