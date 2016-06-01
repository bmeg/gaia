package bmeg.gaea.worker

import bmeg.gaea.feature.Feature

import gremlin.scala._
import com.thinkaurelius.titan.core.TitanGraph
import scalaz._, Scalaz._
import argonaut._, Argonaut._

object SignatureWorker {
  val Name = Key[String]("name")
  val Intercept = Key[Double]("intercept")
  val Expressions = Key[String]("expressions")
  val Coefficient = Key[Double]("coefficient")
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
    val signatures = signatureVertexes.map((vertex) => (vertex, dehydrateCoefficients(vertex.property(Coefficients).orElse(""))))

    for ((signatureVertex, coefficients) <- signatures) {
      for ((feature, coefficient) <- coefficients) {
        val featureVertex = Feature.findFeature(graph) (feature)
        signatureVertex --- ("hasCoefficient", Coefficient -> coefficient) --> featureVertex
      }

      graph.tx.commit()
    }

    signatures
  }

  def dotProduct(coefficients: Vector[Double]) (intercept: Double) (value: Vector[Double]): Double = {
    coefficients.zip(value).foldLeft(intercept) ((total, dot) => total + dot._1 * dot._2)
  }

  def splitMap[A <% Ordered[A], B](m: Map[A, B]): Tuple2[Vector[A], Vector[B]] = {
    val ordered = m.toVector.sortBy(_._1)
    val a = ordered.map(_._1)
    val b = ordered.map(_._2)
    (a, b)
  }

  def selectKeys[A, B](m: Map[A, B]) (keys: Seq[A]) (default: B): Map[A, B] = {
    keys.map(key => (key, m.get(key).getOrElse(default))).toMap
  }

  // def signatureAppliesTo
  //   (features: Vector[String])
  //   (coefficients: Vector[Double])
  //   (intercept: Double)
  //   (threshold: Double)
  //   (expressionVertex: Vertex)
  //     : Boolean = {

  //   val expressions = dehydrateCoefficients(expressionVertex.property(Expressions).orElse(""))
  //   val relevantFeatures = selectKeys[String, Double](expressions) (features) (0.0)
  //   val (genes, levels) = splitMap[String, Double](relevantFeatures)

  //   val dot = dotProduct(coefficients) (intercept) (levels)
  //   dot > threshold
  // }

  // def applySignatureToExpressions
  //   (graph: TitanGraph)
  //   (signature: Tuple2[Vertex, Map[String, Double]])
  //   (expressions: List[Vertex])
  //     : TitanGraph = {

  //   val (signatureVertex, coefficients) = signature
  //   val (features, values) = splitMap[String, Double](coefficients)
  //   val intercept = signatureVertex.property(Intercept).orElse(0.0)

  //   val relevant = expressions.filter(signatureAppliesTo(features) (values) (intercept) (0.5))

  //   for (expressionVertex <- relevant) {
  //     signatureVertex --- ("appliesTo") --> expressionVertex
  //   }

  //   graph.tx.commit()
  //   graph
  // }

  // def applySignaturesToExpressions
  //   (graph: TitanGraph)
  //   (signatures: List[Tuple2[Vertex, Map[String, Double]]])
  //     : TitanGraph = {

  //   val expressions = typeVertexes(graph) ("geneExpression")

  //   for (signature <- signatures) {
  //     applySignatureToExpressions(graph) (signature) (expressions)
  //   }

  //   graph
  // }

  def signatureAppliesTo
    (features: Vector[String])
    (coefficients: Vector[Double])
    (intercept: Double)
    (threshold: Double)
    (expression: Tuple2[Vertex, Map[String, Double]])
      : Boolean = {

    val (vertex, expressions) = expression
    val relevantFeatures = selectKeys[String, Double](expressions) (features) (0.0)
    val (genes, levels) = splitMap[String, Double](relevantFeatures)

    val dot = dotProduct(coefficients) (intercept) (levels)
    dot > threshold
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

    val expressionVertexes = typeVertexes(graph) ("geneExpression")
    val expressions = expressionVertexes.map((vertex) => (vertex, dehydrateCoefficients(vertex.property(Expressions).orElse(""))))

    for (signature <- signatures) {
      applySignatureToExpressions(graph) (signature) (expressions)
    }

    graph
  }
}
