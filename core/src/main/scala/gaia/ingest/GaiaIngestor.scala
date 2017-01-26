package gaia.ingest

import gaia.file._

import scala.io.Source
import java.io.File

trait GaiaIngestor {
  def ingestMessage(label: String) (message: String)

  def ingestFile(label: String) (file: File) {
    for (line <- Source.fromFile(file).getLines) {
      ingestMessage(label) (line)
    }
  }

  def ingestPath(label: String) (path: String) {
    ingestFile(label) (new File(path))
  }

  def ingestUrl(label: String) (url: String) {
    for (line <- Source.fromURL(url).getLines) {
      ingestMessage(label) (line)
    }
  }

  def ingestDirectory(path: String) {
    listFiles(path).foreach { file =>
      val parts = file.getName.split(".")
      val label = parts(parts.size - 2)
      ingestFile(label) (file)
    }
  }
}
