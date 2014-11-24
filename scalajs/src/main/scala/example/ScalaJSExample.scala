package example


import upickle._
import autowire._
import example.API.Post
import shared.AdminAPI
import shared.users.WebsocketMessages._
import org.scalajs.dom.{MessageEvent, Event, WebSocket}
import shared.users.{UserId, User}

import scala.scalajs.js
import js.Dynamic.{ global => g }
import rx._
import org.scalajs.dom
import scalatags.JsDom.all._
import Framework._
import scalajs.concurrent.JSExecutionContext.Implicits.queue

object ScalaJSExample extends js.JSApp {

  private val users: Var[List[User]] = Var(List.empty)

  private val online: Var[Set[UserId]] = Var(Set.empty)

  private var socket: WebSocket = _

  private def handleResponse(msg: WSResponseType) = msg match {
    case NowOnline(uid) => online() += uid
    case NowOffline(uid) => online() -= uid
  }

  private def setupSocket() {
    socket = new WebSocket(g.jsRoutes.controllers.Application.socket().webSocketURL(false).asInstanceOf[String])
    socket.onopen = { (evt: Event) => println("Socket Opened!") }
    socket.onmessage = { (evt: MessageEvent) => println(evt.data)
      val response = upickle.read[WSResponseType](evt.data.asInstanceOf[String])
      handleResponse(response)
    }
  }

  def send(req: WSRequestType): Unit = {
    socket.send(upickle.write(req))
  }

  val usersTag: Rx[HtmlTag] = Rx {
    ul(cls:="users")(users().map { user =>
      li(a(
        href:="#",
        style:={if(online().contains(user.uid)) "color:green" else "color:red" },
        onclick:={ () =>
          val req =
            if(online().contains(user.uid)) BringOffline(user.uid)
            else BringOnline(user.uid)
          send(req)
          println(s"HANDLE CLICK: ${user.gamertag}")
        }
      )(user.gamertag))
    })
  }

  def loadUsers(): Unit = {
    Post[AdminAPI].allUsers().call().map { results =>
      users() = results
    }
  }

  def main(): Unit = {
    setupSocket()
    loadUsers()
    dom.document.getElementById("main-content").appendChild(div(
      usersTag
    ).render)
  }
}
