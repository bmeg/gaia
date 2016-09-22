package gaea.facet

import gaea.titan.Titan
import gaea.titan.Console
import gaea.convoy.Ingest
import gaea.feature.Feature
import gaea.signature.Signature
import gaea.collection.Collection._
import gaea.html.VertexHtml

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.MediaType._
import org.http4s.headers.{`Content-Type`, `Content-Length`}

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scalaz.stream.text
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1
import scalaz.concurrent.Task

import java.io.File
import scala.collection.JavaConversions._
// import scala.collection.JavaConverters._

object GeneFacet extends LazyLogging {
  val graph = Titan.connect(Titan.configuration(Map[String, String]()))
  val Name = Key[String]("name")
  val Coefficients = Key[String]("coefficients")
  val SampleType = Key[String]("sampleType")
  val TumorSite = Key[String]("submittedTumorSite")

  val ilog2 = 1.0 / scala.math.log(2)

  def log2(x: Double): Double = {
    scala.math.log(x) * ilog2
  }

  // def countVertexes(graph: TitanGraph): Map[String, Long] = {
  //   val counts = graph.V.traversal.label.groupCount.toList.get(0)
  //   val labels = counts.keySet.toList.asInstanceOf[List[String]]
  //   labels.foldLeft(Map[String, Long]()) { (countMap, label) =>
  //     countMap + (label.toString -> counts.get(label))
  //   }
  // }

  def findIndividualAttributes(graph: TitanGraph): Set[String] = {
    Titan.typeQuery(graph) ("individual").toList.flatMap(_.valueMap.keys).toSet
  }

  // lazy val vertexCounts = countVertexes(graph)
  lazy val individualAttributes = findIndividualAttributes(graph)

  def puts(line: String): Task[Unit] = Task { println(line) }

  def commit(graph: TitanGraph): Process[Task, Unit] = Process eval_ (Task {
    graph.tx.commit()
  })

  def jNum(value: Double): Json = {
    jNumber(value).getOrElse(jZero)
  }

  def coefficientsToJson(coefficients: Map[String, Double]) (key: String) (value: String): Json = {
    coefficients.foldLeft(jEmptyArray) { (json, coefficient) =>
      val (feature, level) = coefficient
      val pair = (key, jString(feature)) ->: (value, jNum(level)) ->: jEmptyObject
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

  def mapToJson(properties: Map[String, Any]) : Json = {
    properties.map( x => {
      ((x._1), (x._2.toString))
    } ).asJson
  }

  def eventMetadata(eventID: String, eventType: String, datatype: String, weights: Map[String, Double]): Json = {
    val weightsJson = coefficientsToJson(weights) ("feature") ("weight")
    ("eventID", jString(eventID)) ->: ("eventType", jString(eventType)) ->: ("datatype", jString(datatype)) ->: ("featureWeights", weightsJson) ->: jEmptyObject
  }

  def signatureToJson(featureNames: List[String]) (vertex: Vertex): Json = {
    val coefficients = Signature.dehydrateCoefficients(vertex) ("coefficients")
    val relevant = selectKeys[String, Double](coefficients) (featureNames) (0.0)
    val score = relevant.values.foldLeft(0.0) ((s, v) => s + Math.abs(v))
    val signatureName = vertex.property(Name).orElse("no name")
    val metadata = eventMetadata(signatureName, "drug sensitivity signature", "NUMERIC", relevant)
    ("score", jNum(score)) ->: ("signatureMetadata", metadata) ->: jEmptyObject
  }

  def sampleGroupToJson(quartiles: Signature.DistributionQuartiles): Json = {
    val quartilesJson = ("minimum", jNum(quartiles.min)) ->: ("first", jNum(quartiles.lower)) ->: ("second", jNum(quartiles.median)) ->: ("third", jNum(quartiles.upper)) ->: ("maximum", jNum(quartiles.max)) ->: jEmptyObject
    ("size", jNumber(quartiles.size)) ->: ("quartiles", quartilesJson) ->: jEmptyObject
  }

  def significanceToJson(featureNames: List[String]) (vertex: Vertex) (significance: Signature.SignificanceDistribution): Json = {
    val coefficients = Signature.dehydrateCoefficients(vertex) ("coefficients")
    val relevant = selectKeys[String, Double](coefficients) (featureNames) (0.0)
    val signatureName = vertex.property(Name).orElse("no name")
    val metadata = eventMetadata(signatureName, "significance to mutation", "NUMERIC", relevant)
    val featureJson = sampleGroupToJson(significance.feature)
    val backgroundJson = sampleGroupToJson(significance.background)
    ("significance", jNum(significance.significance)) ->: ("sampleGroupDetails", featureJson) ->: ("backgroundGroupDetails", backgroundJson) ->: ("signatureMetadata", metadata) ->: jEmptyObject
  }

  def individualEvent(individualVertex: Vertex) (clinicalNames: List[String]): Json = {
    val metadata = eventMetadata(individualVertex.property(Name).orElse(""), "clinical values", "STRING", Map[String, Double]())
    val relevant = selectKeys[String, Any](individualVertex.valueMap()) (clinicalNames) ("")
    val clinicalJson = propertiesToJson(relevant) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", clinicalJson) ->: jEmptyObject
  }

  def clinicalEvent(individualVertexes: Seq[Vertex]) (clinicalName: String): Json = {
    val metadata = eventMetadata(clinicalName, "clinical", "STRING", Map[String, Double]())
    val properties = individualVertexes.map(vertex => (vertex.property("name").orElse(""), vertex.property(clinicalName).orElse(""))).toMap
    val json = propertiesToJson(properties) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def expressionEvent(expressions: Seq[Tuple3[String, Vertex, Map[String, Double]]]) (gene: String): Json = {
    val individuals = expressions.map(_._1)
    val coefficients = expressions.map(_._3)
    val metadata = eventMetadata(gene, "mrna_expression", "NUMERIC", Map[String, Double]())
    val expression = coefficients.map(coefficient => log2(coefficient.get(gene).getOrElse(0.0)))
    val properties = individuals.zip(expression).toMap
    val json = coefficientsToJson(properties) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def levelEvent(levels: Map[String, Double]) (signature: String): Json = {
    val metadata = eventMetadata(signature, "drug sensitivity score", "NUMERIC", Map[String, Double]())
    val json = coefficientsToJson(levels) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def mutationEvent(mutations: Seq[Tuple3[String, String, String]]) (gene: String): Json = {
    val metadata = eventMetadata(Feature.removePrefix(gene), "mutation call", "STRING", Map[String, Double]())
    val samples = mutations.groupBy(_._1)
    val variants = samples.map { s =>
      val (individual, variants) = s
      (individual, variants.map(_._2).toSet.mkString(","))
    }.toMap

    val json = propertiesToJson(variants) ("sampleID") ("value")
    ("metadata", metadata) ->: ("sampleData", json) ->: jEmptyObject
  }

  def takeHighest(n: Int) (signature: Vertex): List[String] = {
    Signature.dehydrateCoefficients(signature) ("coefficients").toList.sortWith(_._2 > _._2).take(n).map(_._1)
  }

  def longProperty(vertex: Vertex) (property: String): Option[Long] = {
    vertex.valueMap.get(property).map(_.asInstanceOf[Long])
  }

  def individualSurvivalJson(individual: Vertex): Json = {
    val values = individual.valueMap("name", "vitalStatus", "deathDaysTo", "submittedTumorType")

    val json = ("name", jString(values("name").asInstanceOf[String])) ->:
      ("status", jString(values("vitalStatus").asInstanceOf[String])) ->:
      ("tumor", jString(values.get("submittedTumorType").getOrElse("unknown").asInstanceOf[String])) ->:
      jEmptyObject

    values.get("deathDaysTo") match {
      case Some(days) => ("days", jNum(days.asInstanceOf[Long])) ->: json
      case None => json
    }
  }

  val service = HttpService {
    case GET -> Root / "gaea" / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gaea" / "gene" / name =>
      val synonym = Feature.findSynonym(graph) (name).getOrElse {
        "no synonym found"
      }
      Ok(jSingleObject(name, jString(synonym)))

    // case GET -> Root / "gaea" / "vertex" / "counts" =>
    //   Ok(vertexCounts.asJson)

    case GET -> Root / "gaea" / "individual" / "tumor" / tumorType =>
      Ok(graph.V.has(Name, "type:individual").out("hasInstance").has(TumorSite, tumorType).value(Name).toList.asJson)

    case GET -> Root / "gaea" / "feature" / feature / "tumor" / "counts" =>
      Ok(Feature.findTumorCounts(graph) ("feature:" + feature).asJson)

    case request @ POST -> Root / "gaea" / "individual" / "survival" =>
      request.as[Json].flatMap { json =>
        val individualNames = json.as[List[String]].getOr(List[String]())
        val individualVertexes = graph.V.hasLabel("individual").has(Name, within(individualNames:_*)).toList
        val individualJson = individualVertexes.foldLeft(jEmptyArray) {(array, vertex) =>
          individualSurvivalJson(vertex) -->>: array
        }

        Ok(individualJson)
      }

    case GET -> Root / "gaea" / "individual" / "attributes" =>
      Ok(individualAttributes.asJson)

    case request @ POST -> Root / "gaea" / "individual" / "values" =>
      request.as[Json].flatMap { json =>
        val clinicalNames = json.as[List[String]].getOr(List[String]())
        val individuals = Titan.typeQuery(graph) ("individual").toList // .map(_.valueMap)
        val individualJson = clinicalNames.foldLeft(jEmptyArray) { (json, clinical) =>
          clinicalEvent(individuals) (clinical) -->>: json
        }

        Ok(individualJson)
      }

    case request @ POST -> Root / "gaea" / "signature" / "gene" =>
      request.as[Json].flatMap { json => 
        val geneNames = json.as[List[String]].getOr(List[String]())
        val featureVertexes = Feature.synonymsQuery(graph) (geneNames).toList
        val featureNames = featureVertexes.map(feature => Feature.removePrefix(feature.property(Name).orElse("")))
        val signatureVertexes = featureVertexes.flatMap(_.in("hasCoefficient").toList).toSet
        val signatureJson = signatureVertexes.map(signatureToJson(featureNames))
        Ok(signatureJson.asJson)
      }

    case request @ POST -> Root / "gaea" / "signature" / "mutation" =>
      request.as[Json].flatMap { json =>
        val geneNames = json.as[List[String]].getOr(List[String]())
        val featureVertexes = Feature.synonymsQuery(graph) (geneNames).toList
        val featureNames = featureVertexes.map(feature => Feature.removePrefix(feature.property(Name).orElse("")))
        val significance = Signature.variantSignificance(graph) (geneNames).filter(_._2.significance < 0.05)
        val signatureVertexes = graph.V.has(Name, within(significance.keys.toList:_*)).toList
        val signatureJson = signatureVertexes.map { vertex =>
          significanceToJson(featureNames) (vertex) (significance(vertex.property("name").orElse("")))
        }

        Ok(signatureJson.asJson)
      }

    case request @ POST -> Root / "gaea" / "signature" / "sample" =>
      request.as[Json].flatMap { json =>
        val metadata = json.as[Map[String, List[Map[String, String]]]].getOr(Map[String, List[Map[String, String]]]())
        val signatureMetadata = metadata("signatureMetadata")
        val expressionMetadata = metadata("expressionMetadata")
        val clinicalMetadata = metadata("clinicalEventMetadata")
        val mutationMetadata = metadata("mutationEventMetadata")

        val signatureNames = signatureMetadata.map(_("eventID"))
        val expressionNames = expressionMetadata.map(_("eventID"))
        val clinicalNames = clinicalMetadata.map(_("eventID"))
        val mutationNames = mutationMetadata.map(_("eventID"))

        val highestQuery = Signature.highestScoringSamples(graph) (signatureNames) (100) (Order.decr)
        val lowestQuery = Signature.highestScoringSamples(graph) (signatureNames) (100) (Order.incr)
        val query = highestQuery ++ lowestQuery

        val signatureData = query.map(_._1)
        val geneNames = expressionNames ++ signatureData.flatMap(takeHighest(5))

        val individualData = query.map(_._3)
        val individualNames = individualData.map(_.property("name").orElse(""))
        val levelQuery = Signature.individualScores(graph) (individualNames.toList) (signatureNames)

        val expressionData = query.map { q =>
          val (sig, expression, individual) = q
          val coefficients = Signature.dehydrateCoefficients(expression) ("expressions")
          (individual.property("name").orElse(""), expression, coefficients)
        }

        val mutationData = Feature.findVariantsForIndividuals(graph) (individualNames.toList) (mutationNames)
          .groupBy(_._3)

        val levelData = levelQuery.map { q =>
          val (signature, individual, level) = q
          (signature.property("name").orElse(""),
            individual.property("name").orElse(""),
            level.property("level").orElse(0.0))
        }.groupBy(_._1)

        val individualJson = clinicalNames.foldLeft(jEmptyArray) { (json, clinical) =>
          clinicalEvent(individualData.toList) (clinical) -->>: json
        }

        val expressionJson = geneNames.toSet.foldLeft(individualJson) { (json, gene) =>
          expressionEvent(expressionData.toList) (gene) -->>: json
        }

        val mutationJson = mutationData.keys.foldLeft(expressionJson) { (json, gene) =>
          mutationEvent(mutationData(gene).toList) (gene) -->>: json
        }

        val levelJson = levelData.foldLeft(mutationJson) { (json, score) =>
          val (signature, levelTuples) = score
          val levels = levelTuples.map(level => (level._2, level._3)).toMap
          levelEvent(levels) (signature) -->>: json
        }

        Ok(levelJson)
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
      Ok(jNum(1))

    case GET -> Root / "gaea" / "vertex" / "explore" =>
      Ok(VertexHtml.layout(VertexHtml.vertex).toString)
        .withContentType(Some(`Content-Type`(`text/html`)))

    case GET -> Root / "gaea" / "vertex" / "find" / name =>
      try {
        val vertex = graph.V.has(Name, name).head
        val inEdges = groupAs[Edge, String, String](vertex.inE.toList) (_.label) (_.outVertex.value[String]("name"))
        val outEdges = groupAs[Edge, String, String](vertex.outE.toList) (_.label) (_.inVertex.value[String]("name"))

        val out = Map[String, Json](
          "type" -> vertex.label.asJson,
          "properties" -> mapToJson(vertex.valueMap),
          "in" -> inEdges.asJson,
          "out" -> outEdges.asJson
        )

        Ok(out.asJson)
      }

      catch {
        case _: Throwable => Ok(Map[String, Json]().asJson)
      }

    // case GET -> Root / "gaea" / "vertex" / name =>
    //   val vertex = graph.V.has(Name, name).head
    //   val o = vertex.out().value(Name).toList()
    //   val i = vertex.in().value(Name).toList()
    //   val out = Map[String,Json](
    //     "type" -> vertex.label().asJson,
    //     "properties" -> mapToJson(vertex.valueMap),
    //     "out" -> o.asJson,
    //     "in" -> i.asJson
    //   )

    //   Ok(out.asJson)

    case request @ POST -> Root / "gaea" / "console" =>
      request.as[Json].flatMap { query =>
        val queryLens = jObjectPL >=> jsonObjectPL("query") >=> jStringPL
        val line = queryLens.get(query).getOrElse("")

        println(line)

        val result = Console.interpret[Any](line) match {
          case Right(result) => result
          case Left(error) => error
        }

        // val result = try {
        //   Console.interpret[Any](line).toString
        // } catch {
        //   case e: Throwable => println(e.getCause); println(e.printStackTrace); e.getMessage();
        //     // .replaceAll("scala.tools.reflect.ToolBoxError: reflective compilation has failed:", "")
        // }

        println(result)

        Ok(("result" -> jString(result.toString)) ->: jEmptyObject)
      }

    case req @ GET -> "static" /: path =>
      val localPath = new File(new File("./resources/public/static"), path.toString)
      StaticFile.fromFile(localPath, Some(req)).fold(NotFound())(Task.now)

    case req @ GET -> Root =>
      val localPath = new File(new File("./resources/public/static"), "main.html")
      StaticFile.fromFile(localPath, Some(req)).fold(NotFound())(Task.now)
  }
}
