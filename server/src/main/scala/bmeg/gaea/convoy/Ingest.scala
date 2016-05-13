package bmeg.gaea.convoy

import bmeg.gaea.titan.Titan
// import bmeg.gaea.schema.Variant
import bmeg.gaea.feature.Feature
import bmeg.gaea.schema.sample

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import scala.collection.JavaConverters._
// import com.google.protobuf.util.JsonFormat
import com.trueaccord.scalapb.json.JsonFormat
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

  def ingestFeature(graph: TitanGraph) (feature: sample.Feature): Vertex = {
    Feature.findFeature(graph) (feature.name)
  }

  def ingestPosition(graph: TitanGraph) (position: sample.Position): Vertex = {
    val positionVertex = findVertex(graph) ("position") (position.name)
    positionVertex.setProperty(keys("chromosome"), position.chromosome)
    positionVertex.setProperty(keys("strand"), position.strand)
    positionVertex.setProperty(Key[Long]("start"), position.start)
    positionVertex.setProperty(Key[Long]("end"), position.end)
    positionVertex
  }

  def ingestDomain(graph: TitanGraph) (domain: sample.Domain): Vertex = {
    findVertex(graph) ("domain") (domain.name)
  }

  def ingestVariantCallEffect(graph: TitanGraph) (effect: sample.VariantCallEffect): Vertex = {
    val effectVertex = findVertex(graph) ("variantCallEffect") (effect.name)
    effectVertex.setProperty(keys("variantClassification"), effect.variantClassification)
    effectVertex.setProperty(keys("dbsnpRS"), effect.dbsnpRS)
    effectVertex.setProperty(keys("dbsnpValStatus"), effect.dbsnpValStatus)

    for (feature <- effect.inFeatureEdgesFeature) {
      val featureVertex = Feature.findFeature(graph) (feature)
      effectVertex --- ("inFeature") --> featureVertex
    }

    for (domain <- effect.inDomainEdgesDomain) {
      val domainVertex = ingestDomain(graph) (sample.Domain(domain))
      effectVertex --- ("inDomain") --> domainVertex
    }

    for (variant <- effect.effectOfEdgesVariantCall) {
      val variantVertex = findVertex(graph) ("variantCall") (variant)
      effectVertex --- ("effectOf") --> variantVertex
    }

    effectVertex
  }

  def ingestVariantCall(graph: TitanGraph) (variant: sample.VariantCall): Vertex = {
    val variantVertex = findVertex(graph) ("variantCall") (variant.name)

    variantVertex.setProperty(keys("source"), variant.source)
    variantVertex.setProperty(keys("variantType"), variant.variantType)
    variantVertex.setProperty(keys("referenceAllele"), variant.referenceAllele)
    variantVertex.setProperty(keys("normalAllele1"), variant.normalAllele1)
    variantVertex.setProperty(keys("normalAllele2"), variant.normalAllele2)
    variantVertex.setProperty(keys("tumorAllele1"), variant.tumorAllele1)
    variantVertex.setProperty(keys("tumorAllele2"), variant.tumorAllele2)
    variantVertex.setProperty(keys("sequencer"), variant.sequencer)

    for (tumor <- variant.tumorSampleEdgesBiosample) {
      val tumorVertex = findVertex(graph) ("biosample") (tumor)
      variantVertex --- ("tumorSample") --> tumorVertex
    }

    for (normal <- variant.normalSampleEdgesBiosample) {
      val normalVertex = findVertex(graph) ("biosample") (normal)
      variantVertex --- ("normalSample") --> normalVertex
    }

    for (position <- variant.atPositionEdgesPosition) {
      val positionVertex = findVertex(graph) ("position") (position)
      variantVertex --- ("atPosition") --> positionVertex
    }

    variantVertex
  }

  def ingestBiosample(graph: TitanGraph) (biosample: sample.Biosample): Vertex = {
    val biosampleVertex = findVertex(graph) ("biosample") (biosample.name)
    biosampleVertex.setProperty(keys("source"), biosample.source)
    biosampleVertex.setProperty(keys("barcode"), biosample.barcode)
    biosampleVertex.setProperty(keys("sampleType"), biosample.sampleType)

    for (individual <- biosample.sampleOfEdgesIndividual) {
      val individualVertex = findVertex(graph) ("individual") (individual)
      biosampleVertex --- ("sampleOf") --> individualVertex
    }

    biosampleVertex
  }

  def ingestIndividual(graph: TitanGraph) (individual: sample.Individual): Vertex = {
    println("ingesting individual" + individual.name)
    val individualVertex = findVertex(graph) ("individual") (individual.name)
    individualVertex.setProperty(keys("source"), individual.source)

    for ((key, observation) <- individual.observations) {
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

  // def parseIndividual(raw: String): sample.Individual = {
  //   val individual: sample.Individual.Builder = sample.Individual.newBuilder()
  //   JsonFormat.parser().merge(raw, individual)
  //   individual.build()
  // }

  // def parseMessage[T <: com.trueaccord.scalapb.GeneratedMessage](raw: String): T = {
  //   JsonFormat.fromJsonString[T](raw)
  // }

  def ingestMessage(messageType: String) (graph: TitanGraph) (line: String): Task[Vertex] = Task {
    if (messageType == "Feature") {
      val feature = JsonFormat.fromJsonString[sample.Feature](line)
      ingestFeature(graph) (feature)
    } else if (messageType == "Domain") {
      val domain = JsonFormat.fromJsonString[sample.Domain](line)
      ingestDomain(graph) (domain)
    } else if (messageType == "Position") {
      val position = JsonFormat.fromJsonString[sample.Position](line)
      ingestPosition(graph) (position)
    } else if (messageType == "VariantCall") {
      val variantCall = JsonFormat.fromJsonString[sample.VariantCall](line)
      ingestVariantCall(graph) (variantCall)
    } else if (messageType == "VariantCallEffect") {
      val variantCallEffect = JsonFormat.fromJsonString[sample.VariantCallEffect](line)
      ingestVariantCallEffect(graph) (variantCallEffect)
    } else if (messageType == "Biosample") {
      val biosample = JsonFormat.fromJsonString[sample.Biosample](line)
      ingestBiosample(graph) (biosample)
    } else if (messageType == "Individual") {
      val individual = JsonFormat.fromJsonString[sample.Individual](line)
      ingestIndividual(graph) (individual)
    } else {
      findVertex(graph) ("void") ("void")
    }
  }

  // def ingestIndividuals(individuals: List[sample.Individual]): Int = {
  //   val graph = Titan.connect(Titan.configuration(Map[String, String]()))
  //   println(s"Ingesting ${individuals.length} individuals")

  //   val individualVertexes = individuals.map(ingestIndividual(graph))
  //   graph.tx.commit()

  //   println(s"Ingested ${individualVertexes.length} individuals")
  //   individualVertexes.length
  // }

  // def parseIndividualList(raw: String): sample.IndividualList = {
  //   val individualList: sample.IndividualList.Builder = sample.IndividualList.newBuilder()
  //   JsonFormat.parser().merge(raw, individualList)
  //   individualList.build()
  // }

  // def ingestIndividualList(individualList: sample.IndividualList): Int = {
  //   val individuals = individualList.getIndividualsList().asScala.toList
  //   ingestIndividuals(individuals)
  // }
}
