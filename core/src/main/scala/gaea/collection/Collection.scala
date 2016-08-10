package gaea.collection

object Collection {
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

  def distinctPairs[A](items: Iterable[A]): Iterable[Tuple2[A, A]] = {
    for (x <- items; y <- items if x != y) yield (x, y)
  }

  def groupCount[A](items: Iterable[A]): Map[A, Int] = {
    items.groupBy((a) => a).map((t) => (t._1 -> t._2.size)).toMap
  }

  def shear[A](blade: Seq[A], sheep: Seq[A]): List[A] = {
    // only works with sorted seqs where blade is entirely contained within sheep
    sheep.foldLeft((blade, List[A]())) { (wool, sheep) =>
      if (wool._1.isEmpty) {
        (wool._1, wool._2 :+ sheep)
      } else if (wool._1.head == sheep) {
        (wool._1.drop(1), wool._2)
      } else {
        (wool._1, wool._2 :+ sheep)
      }
    }._2
  }
}
