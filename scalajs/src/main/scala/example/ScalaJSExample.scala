package example

import org.scalajs.dom.{MessageEvent, Event, WebSocket}

import scala.scalajs.js
import js.Dynamic.{ global => g }
import rx._
import org.scalajs.dom
import scalatags.JsDom.all._

object ScalaJSExample extends js.JSApp {

  private var socket: WebSocket = _


  private def setupSocket() {
    socket = new WebSocket(g.jsRoutes.controllers.Application.socket().webSocketURL(false).asInstanceOf[String])
    socket.onopen = { (evt: Event) => socket.send("SETUP COMPLETE!") }
    socket.onmessage = { (evt: MessageEvent) => println(evt.data)}

  }

  def main(): Unit = {
    setupSocket()
    dom.document.getElementById("main-content").appendChild(h1("Hello").render)
  }
}
