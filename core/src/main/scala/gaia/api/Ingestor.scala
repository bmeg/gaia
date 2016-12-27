package gaia.api

trait Ingestor {
  type MessageCallback = ( Map[String,Any] ) => Unit
  type CloseCallback = (Int) => Unit

  def setMessageCallback( callback :MessageCallback )
  def setCloseCallback( callback: CloseCallback )

  def start()
  def stop()
}
