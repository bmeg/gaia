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

  def groupAs[A, B, C](items: Iterable[A]) (key: A => B) (value: A => C): Map[B, List[C]] = {
    items.foldLeft(Map[B, List[C]]()) { (group, item) =>
      val k = key(item)
      group + (k -> (value(item) :: group.getOrElse(k, List[C]())))
    }
  }

  def shear[A](blade: Seq[A], sheep: Seq[A]): List[A] = {
    // only works with sorted seqs where the blade is a sublist of the sheep
    sheep.foldLeft((blade, List[A]())) { (wool, sheep) =>
      val blade = wool._1
      val flock = wool._2

      if (blade.isEmpty) {
        (blade, flock :+ sheep)
      } else if (blade.head == sheep) {
        (blade.drop(1), flock)
      } else {
        (blade, flock :+ sheep)
      }
    }._2
  }
}
