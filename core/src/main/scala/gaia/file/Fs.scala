package gaia

import java.io.File

package object file {
  def directoryTree(dir: File): List[File] = {
    if (dir.exists) {
      if (dir.isDirectory) {
        val files = dir.listFiles
        files.filter(_.isFile).toList ++ files.filter(_.isDirectory).flatMap(directoryTree)
      } else {
        List[File](dir)
      }
    } else {
      List[File]()
    }
  }

  def listFiles(path: String): List[File] = {
    val dir = new File(path)
    directoryTree(dir)
  }
}
