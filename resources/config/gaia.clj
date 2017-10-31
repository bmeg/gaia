{:kafka
 {:base
  {:host "localhost"
   :port "9092"}}

 :funnel
 {:host "http://localhost:19191"
  :path "/Users/spanglry/Data/funnel"}

 :store
 {:type :file
  :root "/Users/spanglry/Data/funnel"}

 :flow
 {:path "../biostream/bmeg-etl/bmeg"}}
