package gaia

import java.io.File

package object file {
  def listFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists) {
      if (d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
      } else {
        List[File](d)
      }
    } else {
      List[File]()
    }
  }
}
