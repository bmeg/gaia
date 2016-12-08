package gaia.ingest

import java.util.Properties

import org.apache.kafka.clients.consumer.KafkaConsumer

import collection.JavaConverters._

class Consumer(var server:String) {

  val GAEA_IMPORT_TOPIC = "gaia-import"


  def run() = {
    val props = new Properties()
    props.put("bootstrap.servers", server)
    props.put("group.id", "gaia-ingestor")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    var consumer = new KafkaConsumer[String,String](props)
    consumer.subscribe( (Array[String]{GAEA_IMPORT_TOPIC}).toList.asJava )

    while (true) {
      val records = consumer.poll(100)
      records.asScala.foreach( record => {
        //System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
      })
    }

  }


}
