package gaia.ingest

import gaia.file._

import scala.io.Source
import java.io.File

trait GaiaIngestor {
  def ingestMessage(message: String)

  def ingestFile(file: File) {
    for (line <- Source.fromFile(file).getLines) {
      ingestMessage(line)
    }
  }

  def ingestFile(path: String) {
    ingestFile(new File(path))
  }

  def ingestUrl(url: String) {
    for (line <- Source.fromURL(url).getLines) {
      ingestMessage(line)
    }
  }

  def ingestDirectory(path: String) {
    listFiles(path).foreach(ingestFile)
  }
}
