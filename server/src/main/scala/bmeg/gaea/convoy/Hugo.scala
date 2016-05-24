package bmeg.gaea.convoy

import bmeg.gaea.feature.Feature

import scala.io.Source
import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._

object Hugo {
  val hugoIdKey = Key[String]("hugoId")
  val nameKey = Key[String]("name")
  val descriptionKey = Key[String]("description")
  val chromosomeKey = Key[String]("chromosome")
  val accessionKey = Key[String]("accession")
  val refseqKey = Key[String]("refseq")

  def readHugo(filename: String): List[Array[String]] = {
    val hugoLines = Source.fromFile(filename).getLines
    val header = hugoLines.next
    val allHugos = hugoLines.map(_.split("\t")).toList
    allHugos.filter(_(3) == "Approved")
  }

  def featureConvoy(graph: TitanGraph) (hugo: Array[String]): Vertex = {
    val chromosome = if(hugo.length > 6) hugo(6) else ""
    val accession = if(hugo.length > 7) hugo(7) else ""
    val refseq = if(hugo.length > 8) hugo(8) else ""
    val feature = graph + ("feature",
      hugoIdKey -> hugo(0).replaceFirst("HGNC:", ""),
      nameKey -> hugo(1),
      descriptionKey -> hugo(2),
      chromosomeKey -> chromosome,
      accessionKey -> accession,
      refseqKey -> refseq)

    val otherSynonyms = if(hugo.length > 5 && hugo(5) != "") hugo(5).split(", ") else Array[String]()
    val synonyms = otherSynonyms :+ hugo(1)
    for (synonym <- synonyms) {
      val synonymVertex = graph + ("featureSynonym", nameKey -> synonym)
      synonymVertex --- ("synonymFor") --> feature
    }

    feature
  }

  def hugoConvoy(graph: TitanGraph) (hugos: List[Array[String]]): Integer = {
    for (hugo <- hugos) {
      print(".")
      featureConvoy(graph) (hugo)
    }
    hugos.length
  }

  def hugoMigration(graph: TitanGraph) (hugoFile: String): Integer = {
    val hugos = readHugo(hugoFile)
    val count = hugoConvoy(graph) (hugos)
    graph.tx.commit()
    count
  }
}

