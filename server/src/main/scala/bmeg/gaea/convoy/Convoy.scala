package bmeg.gaea.convoy

import bmeg.gaea.titan.Titan
import bmeg.gaea.schema.Variant

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import scala.collection.JavaConverters._

object Convoy {
  val protobufferKeys = List(
    "name",
    "source",

    "variantClassification",
    "referenceAllele",
    "normalAllele1",
    "normalAllele2",
    "tumorAllele1",
    "tumorAllele2",

    "variantType",
    "transcriptSpecies",
    "transcriptName",
    "transcriptSource",
    "transcriptStatus",
    "transcriptVersion",
    "cPosition",
    "aminoAcidChange",
    "strand"
  )

  val keys = protobufferKeys.foldLeft(Map[String, Key[String]]()) {(m, s) =>
    m + (s -> Key[String](s))
  }

  def ingestDomain(graph: TitanGraph) (domain: String): Vertex = {
    graph.V.hasLabel("domain").has(keys("name"), domain).headOption.getOrElse {
      graph + ("domain", keys("name") -> domain)
    }
  }

  def ingestVariantCallEffect(graph: TitanGraph) (callEffect: Variant.VariantCallEffect): Vertex = {
    val callEffectVertex = graph + ("variantCallEffect",
      keys("variantType") -> callEffect.getVariantType(),
      keys("transcriptSpecies") -> callEffect.getTranscriptSpecies(),
      keys("transcriptName") -> callEffect.getTranscriptName(),
      keys("transcriptSource") -> callEffect.getTranscriptSource(),
      keys("transcriptStatus") -> callEffect.getTranscriptStatus(),
      keys("transcriptVersion") -> callEffect.getTranscriptVersion(),
      keys("cPosition") -> callEffect.getCPosition(),
      keys("aminoAcidChange") -> callEffect.getAminoAcidChange(),
      keys("strand") -> callEffect.getStrand()
    )

    val feature = callEffect.getFeature()
    val featureVertex = graph.V.hasLabel("feature").has(keys("name"), feature).headOption.getOrElse {
      graph + ("feature", keys("name") -> feature)
    }

    featureVertex --- ("hasEffect") --> callEffectVertex

    val domains = callEffect.getDomainsList().asScala.toList
    val domainVertexes = domains.map(ingestDomain(graph))
    for (domainVertex <- domainVertexes) {
      domainVertex --- ("hasEffect") --> callEffectVertex
    }

    callEffectVertex
  }

  def ingestVariantCall(graph: TitanGraph) (source: String) (variantCall: Variant.VariantCall): Vertex = {
    val variantCallVertex = graph + ("variantCall",
      keys("source") -> source,
      keys("variantClassification") -> variantCall.getVariantClassification(),
      keys("referenceAllele") -> variantCall.getReferenceAllele(),
      keys("normalAllele1") -> variantCall.getNormalAllele1(),
      keys("normalAllele2") -> variantCall.getNormalAllele2(),
      keys("tumorAllele1") -> variantCall.getTumorAllele1(),
      keys("tumorAllele2") -> variantCall.getTumorAllele2()
    )

    val callEffects = variantCall.getVariantCallEffectsList().asScala.toList
    val callEffectVertexes = callEffects.map(ingestVariantCallEffect(graph))
    variantCallVertex
  }

  def ingestBioSample(graph: TitanGraph) (source: String) (bioSample: Variant.BioSample): Vertex = {
    val bioSampleVertex = graph.V.hasLabel("bioSample").has(keys("name"), bioSample.getName()).headOption.getOrElse {
      graph + ("bioSample",
        keys("name") -> bioSample.getName(),
        keys("source") -> source
      )
    }

    val variantCalls = bioSample.getVariantCallsList().asScala.toList
    val variantCallVertexes = variantCalls.map(ingestVariantCall(graph) (source))
    for (variantCallVertex <- variantCallVertexes) {
      bioSampleVertex --- ("hasVariantCall") --> variantCallVertex
    }

    bioSampleVertex
  }

  def ingestIndividual(graph: TitanGraph) (individual: Variant.Individual): Vertex = {
    val source = individual.getSource()
    val individualVertex = graph.V.hasLabel("individual").has(keys("name"), individual.getName()).headOption.getOrElse {
      graph + ("individual",
        keys("name") -> individual.getName(),
        keys("source") -> source
      )
    }

    val bioSamples = individual.getBioSamplesList().asScala.toList
    val bioSampleVertexes = bioSamples.map(ingestBioSample(graph) (source))
    for (bioSampleVertex <- bioSampleVertexes) {
      individualVertex --- ("hasSample") --> bioSampleVertex
    }

    individualVertex
  }

  def ingestIndividualList(individualList: Variant.IndividualList): Int = {
    val individuals = individualList.getIndividualsList().asScala.toList
    val graph = Titan.connect(Titan.configuration())
    println(s"Ingesting ${individuals.length} individuals")

    val individualVertexes = individuals.map(ingestIndividual(graph))
    graph.tx.commit()
    individualVertexes.length
  }
}
