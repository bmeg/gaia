
package gaia.test

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest._

class KafkaSuite extends FunSuite with BeforeAndAfter {

  val KAFKA_PORT = 12345
  val KAFKA_URL = "localhost:12345"
  implicit val config = EmbeddedKafkaConfig(kafkaPort = KAFKA_PORT)

  before {
    EmbeddedKafka.start()
  }

  after {
    EmbeddedKafka.stop()
  }
}
