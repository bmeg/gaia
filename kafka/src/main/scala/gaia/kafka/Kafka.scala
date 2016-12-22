package gaia.kafka

import gaia.ingest.GraphTransform
import gaia.graph._
import gaia.file._

import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.util.{Properties, UUID}
import collection.JavaConverters._
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}

object GaiaProducer {
  val mapper = new ObjectMapper()

  def buildProducer(server: String): KafkaProducer[String, String] = {
    val props = new java.util.HashMap[String,Object]()
    props.put("bootstrap.servers", server)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }

  def send(producer: KafkaProducer[String, String], topic: String, message: String): Unit = {
    producer.send(new ProducerRecord[String, String](topic, UUID.randomUUID().toString, message))
  }
}

object GaiaConsumer {
  def buildConsumer(server: String) (groupID: String) (topics: Seq[String]): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", server)
    props.put("group.id", groupID)
    props.put("auto.offset.reset", "earliest")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("request.timeout.ms", "140000")
    props.put("session.timeout.ms", "120000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(topics.toList.asJava)
    consumer
  }

  def run(consumer: KafkaConsumer[String, String], handle: ConsumerRecord[String, String] => Unit) = {
    while (true) {
      val records = consumer.poll(10000)
      records.asScala.foreach( record => {
        println("topic = " + record.topic + ", offset = " + record.offset.toString + ", value = " + record.value.take(20));
        try {
          handle(record)
        } catch {
          case e: Throwable => println("failed! " + e.getMessage)
        }
      })
    }
  }
}

class Spout(server: String) {
  val producer = GaiaProducer.buildProducer(server)
  def spout(path: String, topic: String): Unit = {
    for (file <- listFiles(path)) {
      println("processing file " + file.getName)
      for (line <- Source.fromFile(file).getLines()) {
        GaiaProducer.send(producer, topic, line)
      }
    }
  }
}

class Ingestor(graph: GaiaGraph) (server: String) (groupID: String) (topics: Seq[String]) {
  val ingestor = GraphTransform(graph)
  val consumer = GaiaConsumer.buildConsumer(server) (groupID) (topics)
  def ingest(): Unit = {
    GaiaConsumer.run(consumer, record => ingestor.ingestMessage(record.value))
  }
}
