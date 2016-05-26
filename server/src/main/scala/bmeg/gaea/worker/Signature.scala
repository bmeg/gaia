package bmeg.gaea.worker

import gaea.model.logistic.LogisticRegression
import bmeg.gaea.feature.Feature

import gremlin.scala._
import com.thinkaurelius.titan.core.TitanGraph
import scalaz._, Scalaz._
import argonaut._, Argonaut._

object SignatureWorker {
  val Name = Key[String]("name")
  val Intercept = Key[Double]("intercept")
  val Expressions = Key[String]("expressions")
  val Coefficient = Key[String]("coefficient")
  val Coefficients = Key[String]("coefficients")
  val emptyMap = Map[String, Double]()

  def dehydrateCoefficients(raw: String): Map[String, Double] = {
    Parse.parseOption(raw).map(_.as[Map[String, Double]].getOr(emptyMap)).getOrElse(emptyMap)
  }

  def typeVertexes(graph: TitanGraph) (typ: String): List[Vertex] = {
    graph.V.hasLabel("type").has(Name, "type:" + typ).out("hasInstance").toList
  }

  def linkSignaturesToFeatures(graph: TitanGraph): List[Tuple2[Vertex, Map[String, Double]]] = {
    val signatureVertexes = typeVertexes(graph) ("linearSignature")
    val signatures = signatureVertexes.map(
      (_, dehydrateCoefficients(_.property(Coefficients).orElse(""))))

    for ((signatureVertex, coefficients) <- signatures) {
      for ((feature, coefficient) <- coefficients) {
        val featureVertex = Feature.findFeature(graph) (feature)
        signatureVertex --- ("hasCoefficient", Coefficient -> coefficient) --> featureVertex
      }

      graph.tx.commit()
    }

    signatures
  }

  def splitMap[A, B](m: Map[A, B]): Tuple2[Vector[A], Vector[B]] = {
    val ordered = m.toVector.sortBy(_._1)
    val a = ordered.map(_._1)
    val b = ordered.map(_._2)
    (a, b)
  }

  def selectKeys[A, B](m: Map[A, B]) (keys: List[A]) (default: B): Map[A, B] = {
    keys.map(key => (key, m.get(key).getOrElse(default))).toMap
  }

  def applySignatureToExpressions(graph: TitanGraph)
    (signature: Tuple2[Vertex, Map[String, Double]])
    (expressions: List[Tuple2[Vertex, Map[String, Double]]]): TitanGraph = {

    val (vertex, coefficients) = signature
    val (features, values) = splitMap[String, Double](coefficients)
    val regression = LogisticRegression(values, vertex.property(Intercept).orElse(0.0))

    // iterate through expressions and apply signature, creating an edge if positive
  }

  def applySignaturesToExpressions(graph: TitanGraph) (signatures: List[Tuple2[Vertex, Map[String, Double]]]): TitanGraph = {
    val expressionVertexes = typeVertexes(graph) ("geneExpression")
    val expressions = expressionVertexes.map(
      (_, dehydrateCoefficients(_.property(Expressions).orElse(""))))

    val signatureRegressions = signatures.map(((vertex, coefficients)) => (vertex, coefficients, new LogisticRegression(coefficients.toVector, vertex.property(Intercept).orElse(0.0))))
  }
}
