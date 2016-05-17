package bmeg.gaea.convoy

import bmeg.gaea.titan.Titan
import bmeg.gaea.feature.Feature
import bmeg.gaea.schema.Sample

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import scala.collection.JavaConverters._
import java.lang.{Long => Llong}
import scalaz.concurrent.Task

// RNASeq.geneExp
// *.maf
// .patient.tsv

object Ingest {
  val titanStringKeys = List(
    "name",
    "source",
    "symbol",

    // sample keys
    "tumor",
    "normal",
    "barcode",
    "sampleType",

    // variant keys
    "variantType",
    "referenceAllele",
    "normalAllele1",
    "normalAllele2",
    "tumorAllele1",
    "tumorAllele2",
    "sequencer",
    "tumorSampleUUID",
    "matchedNormSampleUUID",

    // variant effect keys
    "variantClassification",
    "dbsnpRS",
    "dbsnpValStatus",
    "strand",
    "chromosome",

    // intersection of patient fields
    "vitalStatus",
    "icdO3Site",
    "tissueSourceSite",
    "ethnicity",
    "race",
    "bcrPatientUuid",
    "tumorStatus",
    "icd10",
    "gender",
    "patientId",
    "bcrPatientBarcode",
    "icdO3Histology",
    "sample"
  )

  val titanBooleanKeys = List(
    // patient keys
    "prospectiveCollection",
    "retrospectiveCollection",
    "historyOtherMalignancy",
    "historyNeoadjuvantTreatment",
    "primaryLymphNodePresentationAssessment",
    "pretreatmentHistory",
    "krasGeneAnalysisPerformed",
    "informedConsentVerified",
    "lossExpressionOfMismatchRepairProteinsByIhc",
    "priorDiagnosis",
    "historyOfColonPolyps",
    "lymphaticInvasion",
    "brafGeneAnalysisPerformed",
    "venousInvasion",
    "microsatelliteInstability",
    "synchronousColonCancerPresent",
    "perineuralInvasionPresent",
    "nonNodalTumorDeposits",
    "colonPolypsPresent"
  )

  val titanLongKeys = List(
    // position keys
    "start",
    "end",

    // patient keys
    "yearOfFormCompletion",
    "monthOfFormCompletion",
    "dayOfFormCompletion",
    "daysToBirth",
    "yearOfInitialPathologicDiagnosis",
    "daysToInitialPathologicDiagnosis",
    "daysToLastFollowup",
    "daysToLastKnownAlive",
    "daysToIndex",
    "birthDaysTo",
    "deathDaysTo",
    "lastContactDaysTo",
    "ageAtInitialPathologicDiagnosis",
    "numberOfFirstDegreeRelativesWithCancerDiagnosis",
    "lymphNodeExaminedCount",
    "numberOfLymphnodesPositiveByHe",
    "circumferentialResectionMargin",
    "height",
    "weight"
  )

  val titanDoubleKeys = List(
    "preoperativePretreatmentCeaLevel"
  )

  val indexSpec = Map(
    "positionIndex" -> Map(
      "chromosome" -> classOf[String],
      "strand" -> classOf[String],
      "start" -> classOf[Llong],
      "end" -> classOf[Llong]),

    "nameIndex" -> Map("name" -> classOf[String]),

    "symbolIndex" -> Map("symbol" -> classOf[String]),
    "genderIndex" -> Map("gender" -> classOf[String]),
    "cancerIndex" -> Map("cancer" -> classOf[String])
  )

  val keys: Map[String, Key[String]] = titanStringKeys.foldLeft(Map[String, Key[String]]()) {(m, s) =>
    m + (s -> Key[String](s))
  }

  val nkeys: Map[String, Key[Llong]] = titanLongKeys.foldLeft(Map[String, Key[Llong]]()) {(m, s) =>
    m + (s -> Key[Llong](s))
  }

  val bkeys: Map[String, Key[Boolean]] = titanBooleanKeys.foldLeft(Map[String, Key[Boolean]]()) {(m, s) =>
    m + (s -> Key[Boolean](s))
  }

  val dkeys: Map[String, Key[Double]] = titanDoubleKeys.foldLeft(Map[String, Key[Double]]()) {(m, s) =>
    m + (s -> Key[Double](s))
  }

  def camelize(s: String): String = {
    val break = s.split("_")
    val upper = break.head +: break.tail.map(_.capitalize)
    upper.mkString("")
  }

  def findVertex(graph: TitanGraph) (label: String) (name: String): Vertex = {
    graph.V.hasLabel(label).has(keys("name"), name).headOption.getOrElse {
      graph + (label, keys("name") -> name)
    }
  }

  def ingestFeature(graph: TitanGraph) (feature: Sample.Feature): Vertex = {
    Feature.findFeature(graph) (feature.getName())
  }

  def ingestPosition(graph: TitanGraph) (position: Sample.Position): Vertex = {
    val positionVertex = findVertex(graph) ("position") (position.getName())
    positionVertex.setProperty(keys("chromosome"), position.getChromosome())
    positionVertex.setProperty(keys("strand"), position.getStrand())
    positionVertex.setProperty(Key[Long]("start"), position.getStart())
    positionVertex.setProperty(Key[Long]("end"), position.getEnd())
    positionVertex
  }

  def ingestDomain(graph: TitanGraph) (domain: Sample.Domain): Vertex = {
    findVertex(graph) ("domain") (domain.getName())
  }

  def ingestVariantCallEffect(graph: TitanGraph) (effect: Sample.VariantCallEffect): Vertex = {
    val effectVertex = findVertex(graph) ("variantCallEffect") (effect.getName())
    effectVertex.setProperty(keys("variantClassification"), effect.getVariantClassification())
    effectVertex.setProperty(keys("dbsnpRS"), effect.getDbsnpRS())
    effectVertex.setProperty(keys("dbsnpValStatus"), effect.getDbsnpValStatus())

    for (feature <- effect.getInFeatureEdgesFeatureList().asScala.toList) {
      val featureVertex = Feature.findFeature(graph) (feature)
      effectVertex --- ("inFeature") --> featureVertex
    }

    for (domain <- effect.getInDomainEdgesDomainList().asScala.toList) {
      val domainVertex = findVertex(graph) ("domain") (domain)
      effectVertex --- ("inDomain") --> domainVertex
    }

    for (variant <- effect.getEffectOfEdgesVariantCallList().asScala.toList) {
      val variantVertex = findVertex(graph) ("variantCall") (variant)
      effectVertex --- ("effectOf") --> variantVertex
    }

    effectVertex
  }

  def ingestVariantCall(graph: TitanGraph) (variant: Sample.VariantCall): Vertex = {
    val variantVertex = findVertex(graph) ("variantCall") (variant.getName())

    variantVertex.setProperty(keys("source"), variant.getSource())
    variantVertex.setProperty(keys("variantType"), variant.getVariantType())
    variantVertex.setProperty(keys("referenceAllele"), variant.getReferenceAllele())
    variantVertex.setProperty(keys("normalAllele1"), variant.getNormalAllele1())
    variantVertex.setProperty(keys("normalAllele2"), variant.getNormalAllele2())
    variantVertex.setProperty(keys("tumorAllele1"), variant.getTumorAllele1())
    variantVertex.setProperty(keys("tumorAllele2"), variant.getTumorAllele2())
    variantVertex.setProperty(keys("sequencer"), variant.getSequencer())

    for (tumor <- variant.getTumorSampleEdgesBiosampleList().asScala.toList) {
      val tumorVertex = findVertex(graph) ("biosample") (tumor)
      variantVertex --- ("tumorSample") --> tumorVertex
    }

    for (normal <- variant.getNormalSampleEdgesBiosampleList().asScala.toList) {
      val normalVertex = findVertex(graph) ("biosample") (normal)
      variantVertex --- ("normalSample") --> normalVertex
    }

    for (position <- variant.getAtPositionEdgesPositionList().asScala.toList) {
      val positionVertex = findVertex(graph) ("position") (position)
      variantVertex --- ("atPosition") --> positionVertex
    }

    variantVertex
  }

  def ingestBiosample(graph: TitanGraph) (biosample: Sample.Biosample): Vertex = {
    val biosampleVertex = findVertex(graph) ("biosample") (biosample.getName())
    biosampleVertex.setProperty(keys("source"), biosample.getSource())
    biosampleVertex.setProperty(keys("barcode"), biosample.getBarcode())
    biosampleVertex.setProperty(keys("sampleType"), biosample.getSampleType())

    for (individual <- biosample.getSampleOfEdgesIndividualList().asScala.toList) {
      val individualVertex = findVertex(graph) ("individual") (individual)
      biosampleVertex --- ("sampleOf") --> individualVertex
    }

    biosampleVertex
  }

  def ingestIndividual(graph: TitanGraph) (individual: Sample.Individual): Vertex = {
    println("ingesting individual" + individual.getName())
    val individualVertex = findVertex(graph) ("individual") (individual.getName())
    individualVertex.setProperty(keys("source"), individual.getSource())

    val observations = individual.getObservations().asScala
    for ((key, observation) <- observations) {
      val observationKey = camelize(key)
      println(observationKey)
      if (nkeys.contains(observationKey)) {
        val raw: String = observation.split("\\.").head
        val n: Llong = Llong.parseLong(raw)
        individualVertex.setProperty(nkeys(observationKey), n)
      } else if (dkeys.contains(observationKey)) {
        val n: Double = java.lang.Double.parseDouble(observation)
        individualVertex.setProperty(dkeys(observationKey), n)
      } else if (bkeys.contains(observationKey)) {
        val bool: Boolean = observation.toLowerCase == "yes"
        individualVertex.setProperty(bkeys(observationKey), bool)
      } else {
        individualVertex.setProperty(keys.get(observationKey).getOrElse {
          Key[String](observationKey)
        }, observation)
      }
    }

    individualVertex
  }

  def ingestGeneExpression(graph: TitanGraph) (expression: Sample.GeneExpression): Vertex = {
    val expressionVertex = findVertex(graph) ("geneExpression") (expression.getName())
    expressionVertex
  }

  def ingestMessage(messageType: String) (graph: TitanGraph) (line: String): Task[Vertex] = Task {
    if (messageType == "Feature") {
      val feature = Parse.parseFeature(line)
      ingestFeature(graph) (feature)
    } else if (messageType == "Domain") {
      val domain = Parse.parseDomain(line)
      ingestDomain(graph) (domain)
    } else if (messageType == "Position") {
      val position = Parse.parsePosition(line)
      ingestPosition(graph) (position)
    } else if (messageType == "VariantCall") {
      val variantCall = Parse.parseVariantCall(line)
      ingestVariantCall(graph) (variantCall)
    } else if (messageType == "VariantCallEffect") {
      val variantCallEffect = Parse.parseVariantCallEffect(line)
      ingestVariantCallEffect(graph) (variantCallEffect)
    } else if (messageType == "Biosample") {
      val biosample = Parse.parseBiosample(line)
      ingestBiosample(graph) (biosample)
    } else if (messageType == "Individual") {
      val individual = Parse.parseIndividual(line)
      ingestIndividual(graph) (individual)
    } else if (messageType == "GeneExpression") {
      val geneExpression = Parse.parseGeneExpression(line)
      ingestGeneExpression(graph) (geneExpression)
    } else {
      findVertex(graph) ("void") ("void")
    }
  }
}
