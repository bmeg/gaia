package gaea.kafka

import gaea.ingest.Ingest
import gaea.titan.Titan

import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.util.{Properties, UUID}
import collection.JavaConverters._
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}

object GaeaProducer {
  val mapper = new ObjectMapper()

  def buildProducer(server: String): KafkaProducer[String, String] = {
    val props = new java.util.HashMap[String,Object]()
    props.put("bootstrap.servers", server)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    // props.put("partitioner.class", "example.producer.SimplePartitioner")

    new KafkaProducer[String, String](props)
  }

  def send(producer: KafkaProducer[String, String], topic: String, message: String): Unit = {
    // val json = mapper.writeValueAsString(message)
    producer.send(new ProducerRecord[String, String](topic, UUID.randomUUID().toString, message))
  }
}

object GaeaConsumer {
  def buildConsumer(server: String, groupID: String, topics: Seq[String]): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", server)
    props.put("group.id", groupID) // "gaea-ingestor")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(topics.toList.asJava)
    consumer
  }

  def run(consumer: KafkaConsumer[String, String], handle: ConsumerRecord[String, String] => Unit) = {
    while (true) {
      val records = consumer.poll(100)
      records.asScala.foreach( record => {
        println("offset = " + record.offset.toString + ", value = " + record.value.take(20));
        handle(record)
      })
    }
  }
}

class Spout(server: String) {
  val producer = GaeaProducer.buildProducer(server)

  def spout(path: String, topic: String): Unit = {
    for (line <- Source.fromFile(path).getLines()) {
      println(line.take(20))
      GaeaProducer.send(producer, topic, line)
    }
  }
}

class Ingestor(server: String, groupID: String, topics: Seq[String]) {
  val consumer = GaeaConsumer.buildConsumer(server, groupID, topics)
  val graph = Titan.defaultGraph()

  def ingest(): Unit = {
    GaeaConsumer.run(consumer, record => Ingest.ingestVertex(graph) (parse(record.value)))
  }
}
