(def config (config/load-config "resources/config/test.clj"))
(def state (boot config))
(def processes (load-processes! state :triangle "resources/test/triangle/triangle.processes.yaml"))
(initiate-flow! state :triangle)
