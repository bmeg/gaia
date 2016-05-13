package bmeg.gaea.convoy

import bmeg.gaea.schema.Sample
import com.google.protobuf.util.JsonFormat

object Parse {
  def parseFeature(raw: String): Sample.Feature = {
    val feature: Sample.Feature.Builder = Sample.Feature.newBuilder()
    JsonFormat.parser().merge(raw, feature)
    feature.build()
  }

  def parseDomain(raw: String): Sample.Domain = {
    val domain: Sample.Domain.Builder = Sample.Domain.newBuilder()
    JsonFormat.parser().merge(raw, domain)
    domain.build()
  }

  def parsePosition(raw: String): Sample.Position = {
    val position: Sample.Position.Builder = Sample.Position.newBuilder()
    JsonFormat.parser().merge(raw, position)
    position.build()
  }

  def parseVariantCallEffect(raw: String): Sample.VariantCallEffect = {
    val variantCallEffect: Sample.VariantCallEffect.Builder = Sample.VariantCallEffect.newBuilder()
    JsonFormat.parser().merge(raw, variantCallEffect)
    variantCallEffect.build()
  }

  def parseVariantCall(raw: String): Sample.VariantCall = {
    val variantCall: Sample.VariantCall.Builder = Sample.VariantCall.newBuilder()
    JsonFormat.parser().merge(raw, variantCall)
    variantCall.build()
  }

  def parseBiosample(raw: String): Sample.Biosample = {
    val biosample: Sample.Biosample.Builder = Sample.Biosample.newBuilder()
    JsonFormat.parser().merge(raw, biosample)
    biosample.build()
  }

  def parseIndividual(raw: String): Sample.Individual = {
    val individual: Sample.Individual.Builder = Sample.Individual.newBuilder()
    JsonFormat.parser().merge(raw, individual)
    individual.build()
  }
}
