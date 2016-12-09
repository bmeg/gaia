package gaia.ingest

import gaia.graph._

trait GaiaIngestor {
  def ingestMessage(message: String)

  def ingestFile(path: String) {
    // Source.fromFile(path).getLines.foldMap(line => ingestMessage(line))
    for (line <- Source.fromFile(path).getLines) {
      ingestMessage(line)
    }
  }
}
