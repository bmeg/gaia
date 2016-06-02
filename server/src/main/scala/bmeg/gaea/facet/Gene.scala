package bmeg.gaea.facet

import bmeg.gaea.titan.Titan
import bmeg.gaea.schema.Variant
import bmeg.gaea.convoy.Ingest
import bmeg.gaea.feature.Feature
import bmeg.gaea.worker.SignatureWorker

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order

import com.typesafe.scalalogging._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scalaz.stream.text
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1
import scalaz.concurrent.Task

object GeneFacet extends LazyLogging {
  val graph = Titan.connect(Titan.configuration(Map[String, String]()))
  val Name = Key[String]("name")
  val Coefficients = Key[String]("coefficients")

  def puts(line: String): Task[Unit] = Task { println(line) }

  def commit(graph: TitanGraph): Process[Task, Unit] = Process eval_ (Task {
    graph.tx.commit()
  })

  def coefficientsToJson(coefficients: Map[String, Double]) (key: String) (value: String): Json = {
    coefficients.foldLeft(jEmptyArray) { (json, coefficient) =>
      val (feature, level) = coefficient
      val pair = (key, jString(feature)) ->: (value, jNumber(level).getOrElse(jZero)) ->: jEmptyObject
      pair -->>: json
    }
  }

  def propertiesToJson(properties: Map[String, Any]) (key: String) (value: String): Json = {
    properties.foldLeft(jEmptyArray) { (json, property) =>
      val (name, attribute) = property
      val pair = (key, jString(name)) ->: (value, jString(attribute.toString)) ->: jEmptyObject
      pair -->>: json
    }
  }

  def eventMetadata(eventID: String, eventType: String, datatype: String, weights: Map[String, Double]): Json = {
    val weightsJson = coefficientsToJson(weights) ("feature") ("weight")
    ("eventID", jString(eventID)) ->: ("eventType", jString(eventType)) ->: ("datatype", jString(datatype)) ->: ("featureWeights", weightsJson) ->: jEmptyObject
  }

  def signatureToJson(featureNames: List[String]) (vertex: Vertex): Json = {
    val coefficients = SignatureWorker.dehydrateCoefficients(vertex.property(Coefficients).orElse(""))
    val relevant = SignatureWorker.selectKeys[String, Double](coefficients) (featureNames) (0.0)
    val score = relevant.values.foldLeft(0.0) (_ + _)
    val signatureName = vertex.property(Name).orElse("no name")
    val metadata = eventMetadata(signatureName, "drug sensitivity signature", "NUMERIC", relevant)
    ("score", jNumber(score).getOrElse(jZero)) ->: ("signatureMetadata", metadata) ->: jEmptyObject
  }

  def expressionEvent(expressionVertex: Vertex) (level: Double) (geneNames: List[String]): Json = {
    val metadata = eventMetadata(expressionVertex.property(Name).orElse(""), "expression values", "NUMERIC", Map[String, Double]())
    val coefficients = SignatureWorker.dehydrateCoefficients(expressionVertex.property(Coefficients).orElse(""))
    val relevant = SignatureWorker.selectKeys[String, Double](coefficients) (geneNames) (0.0) + ("signatureScore" -> level)
    val sampleJson = coefficientsToJson(relevant) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", sampleJson) ->: jEmptyObject
  }

  def individualEvent(individualVertex: Vertex) (clinicalNames: List[String]): Json = {
    val metadata = eventMetadata(individualVertex.property(Name).orElse(""), "clinical values", "STRING", Map[String, Double]())
    val relevant = SignatureWorker.selectKeys[String, Any](individualVertex.valueMap()) (clinicalNames) ("null")
    val clinicalJson = propertiesToJson(relevant) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", clinicalJson) ->: jEmptyObject
  }

  val service = HttpService {
    case GET -> Root / "gaea" / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gaea" / "gene" / name =>
      val synonym = Feature.findSynonym(graph) (name).getOrElse {
        "no synonym found"
      }
      Ok(jSingleObject(name, jString(synonym)))

    case request @ POST -> Root / "gaea" / "signature" / "gene" =>
      request.as[Json].flatMap { json => 
        val geneNames = json.as[List[String]].getOr(List[String]())
        val featureVertexes = geneNames.map(Feature.findSynonymVertex(graph) (_)).flatten
        val featureNames = featureVertexes.map(_.property(Name).orElse(""))
        val signatureVertexes = featureVertexes.flatMap(_.in("hasCoefficient").toList).toSet
        val signatureJson = signatureVertexes.map(signatureToJson(featureNames))
        Ok(signatureJson.asJson)
      }

    case request @ POST -> Root / "gaea" / "signature" / "sample" =>
      request.as[Json].flatMap { json =>
        val metadata = json.as[Map[String, List[Map[String, String]]]].getOr(Map[String, List[Map[String, String]]]())
        val signatureMetadata = metadata("signatureMetadata")
        val expressionMetadata = metadata("expressionMetadata")
        val clinicalEventMetadata = metadata("clinicalEventMetadata")

        val signatureNames = signatureMetadata.map(_("eventID"))
        val expressionNames = expressionMetadata.map(_("eventID"))
        val clinicalNames = clinicalEventMetadata.map(_("eventID"))

        val signatureVertexes = signatureNames.flatMap(graph.V.hasLabel("linearSignature").has(Name, _).headOption)
        val signatureLevels = signatureVertexes.map { (signatureVertex) =>
          val expressionEdges = signatureVertex.outE("appliesTo").orderBy("level", Order.decr).limit(50)
          val expressionVertexes = expressionEdges.toList.map(_.in.head)
          val individualVertexes = expressionVertexes.map(_.out("expressionFor").out("sampleOf").head)
          (signatureVertex, expressionEdges, expressionVertexes, individualVertexes)
        }

        val response = signatureLevels.foldLeft(jEmptyArray) { (json, signature) =>
          val (signatureVertex, expressionEdges, expressionVertexes, individualVertexes) = signature
          val signatureName = signatureVertex.property(Name).orElse("")
          val levels = expressionEdges.map(_.value("level").headOption.getOrElse(0.0))

          val expressionEvents = expressionVertexes.zip(levels).zip(individualVertexes).map { sample =>
            val (expression, individualVertex) = sample
            val (expressionVertex, level) = expression
            val eEvent = expressionEvent(expressionVertex) (level) (expressionNames)
            val iEvent = individualEvent(individualVertex) (clinicalNames)
            ("expressionData", eEvent) ->: ("clinicalData", iEvent) ->: jEmptyObject
          }.asJson

          val signatureEvent = ("signatureName", signatureName) ->: ("topExpressions", expressionEvents) ->: jEmptyObject
          signatureEvent -->>: json
        }

        Ok(response)
      }

    case request @ POST -> Root / "gaea" / "message" / messageType =>
      logger.info("importing " + messageType)
      val messages = request.bodyAsText.pipe(text.lines(1024 * 1024 * 64)).flatMap { line =>
        Process eval Ingest.ingestMessage(messageType) (graph) (line)
      } 
      messages.runLog.run
      Ingest.retryCommit(graph) (5)

      Ok(jString("done!"))

    case request @ POST -> Root / "yellow" =>
      val y = request.bodyAsText.pipe(text.lines()).flatMap { line =>
        Process eval puts(line)
      }
      y.runLog.run
      Ok(jNumber(1))
  }
}
