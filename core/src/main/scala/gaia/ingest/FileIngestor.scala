package gaia.api.ingest

import gaia.api.Ingestor
import gaia.io.JsonIO

import scala.io.Source

class FileIngestor(file: String) extends Ingestor {
  var onMessage : MessageCallback = null
  var onClose : CloseCallback = null

  def setMessageCallback(callback: MessageCallback): Unit = {
    onMessage = callback
  }

  override def start() = {
    new Thread(new Runnable {
      def run() = {
        Source.fromFile(file).getLines().foreach(x => {
          onMessage(JsonIO.readMap(x))
        })
        if (onClose != null) {
          onClose(0)
        }
      }
    }).start()
  }

  def setCloseCallback(callback: CloseCallback) = {
    onClose = callback
  }

  override def stop(): Unit = {
    //BUG: should do something to stop the loop here....
  }
}
