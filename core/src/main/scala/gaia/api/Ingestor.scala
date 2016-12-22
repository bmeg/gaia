package gaia.api


trait Ingestor {

  type MessageCallback = ( java.util.Map[String,Object] ) => Unit
  type CloseCallback = (Int) => Unit

  def setMessageCallback( callback :MessageCallback )
  def setCloseCallback( callback: CloseCallback )

  def start()
  def stop()

}
