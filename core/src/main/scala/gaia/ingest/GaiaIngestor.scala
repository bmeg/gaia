package gaia.ingest

import scala.io.Source

trait GaiaIngestor {
  def ingestMessage(message: String)

  def ingestFile(path: String) {
    // Source.fromFile(path).getLines.foldMap(line => ingestMessage(line))
    for (line <- Source.fromFile(path).getLines) {
      ingestMessage(line)
    }
  }
}
