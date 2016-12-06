package gaea.api.ingest

import gaea.api.Ingestor
import gaea.io.JsonIO

import scala.io.Source


class FileIngestor(file: String) extends Ingestor {
  var onMessage : MessageCallback = null
  var onClose : CloseCallback = null

  def setMessageCallback(callback: MessageCallback): Unit = {
    onMessage = callback
  }

  override def start() = {
    new Thread(new Runnable {
      var io = new JsonIO
      def run() = {
        Source.fromFile(file).getLines().foreach(x => {
          onMessage(io.ReadMap(x))
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
