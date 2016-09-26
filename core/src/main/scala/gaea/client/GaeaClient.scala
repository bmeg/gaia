package gaea.client

import java.util.{Properties, UUID}

import com.fasterxml.jackson.databind.ObjectMapper
import gaea.io.JsonIO
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
//import org.apache.kafka.clients.producer.ProducerConfig

class ConnectionConfig {
  var titanURL: String = null
  var kafkaURL: String = null

  def Kafka(url: String) : ConnectionConfig = {
    this.kafkaURL = url
    return this
  }

  def Titan(url: String) : ConnectionConfig = {
    this.titanURL = url
    return this
  }

}


class GaeaClient(var config: ConnectionConfig) {

  val GAEA_IMPORT_TOPIC = "gaea-import"
  val jsonio = new JsonIO()
  var producer : Producer[String,String] = null

  def kafkaProducerConnect(): Producer[String,String] = {
    val props = new java.util.HashMap[String,Object]()
    props.put("bootstrap.servers", config.kafkaURL)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    //props.put("partitioner.class", "example.producer.SimplePartitioner")
    val producer = new KafkaProducer[String, String](props)
    return producer
  }


  def addMessage(message: java.util.Map[Object,Object]) = {
    val json = jsonio.WriteMap(message)
    if (producer == null) {
      producer = kafkaProducerConnect()
    }
    printf("SEnding: %s", json)
    producer.send(new ProducerRecord[String, String](GAEA_IMPORT_TOPIC, UUID.randomUUID().toString, json))
  }

  def close() = {
    if (producer != null) {
      producer.close()
    }
  }

}