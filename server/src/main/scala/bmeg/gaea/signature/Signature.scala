package bmeg.gaea.signature

import bmeg.gaea.feature.Feature
import bmeg.gaea.titan.Titan
import gaea.math.Stats
import gaea.collection.Collection._

import gremlin.scala._
import com.thinkaurelius.titan.core.TitanGraph
import scalaz._, Scalaz._
import argonaut._, Argonaut._

object Signature {
  val Name = Key[String]("name")
  val Intercept = Key[Double]("intercept")
  val Expressions = Key[String]("expressions")
  val Coefficient = Key[Double]("coefficient")
  val Coefficients = Key[String]("coefficients")
  val Level = Key[Double]("level")

  val emptyMap = Map[String, Double]()

  def dehydrateCoefficients(vertex: Vertex) (key: String): Map[String, Double] = {
    val raw = vertex.property(key).orElse("")
    Parse.parseOption(raw).map(_.as[Map[String, Double]].getOr(emptyMap)).getOrElse(emptyMap)
  }

  def findSignatures(graph: TitanGraph): List[Tuple2[Vertex, Map[String, Double]]] = {
    val signatureVertexes = Titan.typeVertexes(graph) ("linearSignature")
    signatureVertexes.map((vertex) => (vertex, dehydrateCoefficients(vertex) ("coefficients")))
  }

  def linkSignaturesToFeatures(graph: TitanGraph): List[Tuple2[Vertex, Map[String, Double]]] = {
    val signatures = findSignatures(graph)
    for ((signatureVertex, coefficients) <- signatures) {
      for ((feature, coefficient) <- coefficients) {
        val featureVertex = Feature.findFeature(graph) (feature)
        signatureVertex --- ("hasCoefficient", Coefficient -> coefficient) --> featureVertex
      }

      graph.tx.commit()
    }

    signatures
  }

  def signatureLevel
    (features: Vector[String])
    (coefficients: Vector[Double])
    (intercept: Double)
    (expressions: Map[String, Double])
      : Double = {

    val relevantFeatures = selectKeys[String, Double](expressions) (features) (0.0)
    val (genes, levels) = splitMap[String, Double](relevantFeatures)

    dotProduct(coefficients) (intercept) (levels)
  }

  def signatureAppliesTo
    (features: Vector[String])
    (coefficients: Vector[Double])
    (intercept: Double)
    (threshold: Double)
    (expression: Tuple2[Vertex, Map[String, Double]])
      : Boolean = {

    val (vertex, expressions) = expression
    signatureLevel(features) (coefficients) (intercept) (expressions) > threshold
  }

  def applyExpressionToSignatures
    (graph: TitanGraph)
    (expressionVertex: Vertex)
    (signatures: List[Tuple2[Vertex, Map[String, Double]]])
      : TitanGraph = {

    val levels = dehydrateCoefficients(expressionVertex) ("expressions")
    val normalized = Stats.exponentialNormalization(levels)

    for (signature <- signatures) {
      val (signatureVertex, coefficients) = signature
      val intercept = signatureVertex.property(Intercept).orElse(0.0)
      val (features, values) = splitMap[String, Double](coefficients)
      val level = signatureLevel(features) (values) (intercept) (normalized)

      signatureVertex --- ("appliesTo", Level -> level) --> expressionVertex
    }

    graph.tx.commit()
    graph
  }

  def applyExpressionsToSignatures
    (graph: TitanGraph)
    (signatures: List[Tuple2[Vertex, Map[String, Double]]])
      : TitanGraph = {

    val expressionVertexes = Titan.typeVertexes(graph) ("geneExpression")

    for (expressionVertex <- expressionVertexes) {
      applyExpressionToSignatures(graph) (expressionVertex) (signatures)
    }

    graph
  }

  def applySignatureToExpressions
    (graph: TitanGraph)
    (signature: Tuple2[Vertex, Map[String, Double]])
    (expressions: List[Tuple2[Vertex, Map[String, Double]]])
      : TitanGraph = {

    val (signatureVertex, coefficients) = signature
    val (features, values) = splitMap[String, Double](coefficients)
    val intercept = signatureVertex.property(Intercept).orElse(0.0)

    val relevant = expressions.filter(signatureAppliesTo(features) (values) (intercept) (0.5))

    for (expression <- relevant) {
      val (expressionVertex, levels) = expression
      signatureVertex --- ("appliesTo") --> expressionVertex
    }

    graph.tx.commit()
    graph
  }

  def applySignaturesToExpressions
    (graph: TitanGraph)
    (signatures: List[Tuple2[Vertex, Map[String, Double]]])
      : TitanGraph = {

    val expressionVertexes = Titan.typeVertexes(graph) ("geneExpression")
    val expressions = expressionVertexes.map((vertex) => (vertex, dehydrateCoefficients(vertex) ("expressions")))

    for (signature <- signatures) {
      applySignatureToExpressions(graph) (signature) (expressions)
    }

    graph
  }
}
