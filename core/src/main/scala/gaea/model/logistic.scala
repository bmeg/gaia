package gaea.model.logistic

import scala.math
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Vector


/** Functions required for logistic regression prediction. */  
object Utils {
  
  /** Apply logistic function to the value.
    * 
    * @param t the value to apply logistic function.
    */
  def logisticFunction(t: Double): Double = {
    1.0 / (1.0 + scala.math.pow(scala.math.E, -t))
  }

  /** Calculate dot product of two vectors.
    *
    * @param value a vector that holds one set of values
    * @param coef a vector that holds coefficients
    * @param intercept the intercept to be added to the dot product.
    */
  def dotProduct(value: Vector[Double], coef: Vector[Double], intercept: Double): Double = {
    var dotProduct = intercept

    for ( i <- 0 to value.length -1 ) {
      dotProduct += ( value(i) * coef(i) )
    }
    dotProduct
  }

  /** Calculate dot products.
    * 
    * @param doubleVectors a vector of vectors that holds multiple sets of values.
    * @param coef a vector that holds coefficients
    * @param intercept the intercept to be added to the dot product.
    *                  (default = 0)
    */
  def dotProductOfVectors(doubleVectors: Vector[Vector[Double]], coef: Vector[Double],
                      intercept: Double = 0): Vector[Double] = {
    var mutableDotProductList = new ListBuffer[Double]() 

    for ( i <- 0 to doubleVectors.length -1 ) {
      mutableDotProductList += dotProduct(doubleVectors(i), coef, intercept)
    }
    mutableDotProductList.toVector
  }

}


/** Logistic regression model used for prediction.
  *
  * @param coef the vector of coefficients 
  *             for the logistic regression model.
  * @param intercept the intercept of the logistic regression model.
  */
class LogisticRegression(coef: Vector[Double], intercept: Double) {

  /** Decision function that returns confidence of predicting '1'.
    *
    * @param data the data to make predictions on.
    */
  def decisionFunction(data: Vector[Vector[Double]]): Vector[Double] = {
    Utils.dotProductOfVectors(data, coef, intercept)
  }

  /** Probability of predicting '1'.
      *
      * @param data the data to make predictions on.
      */
  def predictProba(data: Vector[Vector[Double]]): Vector[Double] = {
    val confidence = decisionFunction(data)
    var mutableProbability = new ListBuffer[Double]()

    for ( i <- 0 to confidence.length -1 ) {
      mutableProbability += Utils.logisticFunction(confidence(i))
    }
    mutableProbability.toVector
  }

  /** Predict binary state, represented in '1' and '0'.
    *
    * @param data the data to make predictions on.
    * @param threshold the value whether that is used 
    *                  as a threshold for determining '1' or '0'.
    *                  (default = 0.5)
    */
  def predict(data: Vector[Vector[Double]], threshold: Double = 0.5): Vector[Int] = {
    val confidence = decisionFunction(data)
    var mutablePrediction = new ListBuffer[Int]()

    for (i <- 0 to confidence.length -1 ) {
      if (confidence(i) > threshold) mutablePrediction += 1
      else mutablePrediction += 0
    }
    mutablePrediction.toVector
  }

}
