package gaia.api


trait Ingestor {

  type MessageCallback = ( java.util.Map[Object,Object] ) => Unit
  type CloseCallback = (Int) => Unit

  def setMessageCallback( callback :MessageCallback )
  def setCloseCallback( callback: CloseCallback )

  def start()
  def stop()

}
