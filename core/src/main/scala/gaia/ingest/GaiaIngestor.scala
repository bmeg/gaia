package gaia.ingest

import gaia.file._

import scala.io.Source
import java.io.File

trait GaiaIngestor {
  def ingestMessage(label: String) (message: String)

  def findLabel(s: String): String = {
    val parts = s.split('.')
    if (parts.size > 1) {
      parts(parts.size - 2)
    } else {
      s
    }
  }

  def ingestFile(file: File) {
    val label = findLabel(file.getName)
    ingestFile(label, file)
  }

  def ingestFile(label: String, file: File) {
    for (line <- Source.fromFile(file).getLines) {
      ingestMessage(label) (line)
    }
  }

  def ingestPath(path: String) {
    ingestFile(new File(path))
  }

  def ingestPath(label: String, path: String) {
    ingestFile(label, new File(path))
  }

  def ingestUrl(url: String) {
    val label = findLabel(url)
    ingestUrl(label, url)
  }

  def ingestUrl(label: String, url: String) {
    for (line <- Source.fromURL(url).getLines) {
      ingestMessage(label) (line)
    }
  }

  def ingestDirectory(path: String) {
    listFiles(path).foreach { file =>
      ingestFile(file)
    }
  }
}
