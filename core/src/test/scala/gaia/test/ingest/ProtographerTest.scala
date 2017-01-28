package gaia.test.ingest

import gaia.ingest._
import gaia.io.JsonIO

import org.scalatest._

class GidTest extends FunSuite {
  // val template = new GidTemplate("variant:{{referenceName}}:{{start}}:{{end}}:{{referenceBases}}:{{#join}}{{alternateBases}}{{/join}}")
  // val template = new GidTemplate("variant:{{referenceName}}:{{start}}:{{end}}:{{referenceBases}}:{{#alternateBases}}{{.}},{{/alternateBases}}")
  val template = new GidTemplate("variant:{{referenceName}}:{{start}}:{{end}}:{{referenceBases}}:{{alternateBases}}")
  val rawVariant = """{"info": {"center": ["broad.mit.edu"]}, "end": "10521380", "calls": [{"callSetId": "callSet:CCK81_LARGE_INTESTINE"}], "referenceBases": "A", "start": "10521380", "alternateBases": ["-"], "referenceName": "1", "id": "variant:1:10521380:10521380:A:-"}"""

  val variant = JsonIO.readMap(rawVariant)
  val gid = template.render(variant)
  assert(gid == "variant:1:10521380:10521380:A:-")
}
