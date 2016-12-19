
graph_proto : 
	protoc --java_out=core/src/main/java ./core/src/main/proto/gaia/schema/GaiaSchema.proto
	protoc --java_out=core/src/main/java ./core/src/main/proto/gaia/schema/ProtoGraph.proto