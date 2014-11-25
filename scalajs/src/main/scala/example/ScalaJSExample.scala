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

  private val friends: Var[Map[UserId, List[UserId]]] = Var(Map.empty)

  private var socket: WebSocket = _

  private def addFriend(a: UserId, b: UserId): Unit = {
    val current: List[UserId] = friends.now.getOrElse(a,List.empty[UserId])
    friends() = friends.now + ((a,b::current))
  }
  private def handleResponse(msg: WSResponseType) = msg match {
    case NowOnline(uid) => online() += uid
    case NowOffline(uid) => online() -= uid
    case UserModified(user) => users() = user :: users().filter(_.uid != user.uid)
    case NowFriends(a,b) => {
      addFriend(a,b)
      addFriend(b,a)
    }
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

  val friendInputA = input(`type`:="text", placeholder:="Make Friends (A)").render
  val friendInputB = input(`type`:="text", placeholder:="Make Friends (A)").render
  val makeFriendsForm = form(
    onsubmit:={() =>
      val a = UserId(friendInputA.value.toLong)
      val b = UserId(friendInputB.value.toLong)
      Post[AdminAPI].makeFriends(a,b).call()
      false
    }
  )(
    friendInputA,
    friendInputB,
    input(`type`:="submit", value:="Make Friends")
  ).render

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
      )(
        s"${user.gamertag} [FOO: ${user.some_foo}}]",
        ul(friends().get(user.uid).map { mahfriends =>
          mahfriends.map { fid =>
            users.now.find(_.uid == fid).map { friend =>
              li(
                style:={if(online().contains(friend.uid)) "color:green" else "color:red" },
                s"${friend.gamertag} (${friend.uid}}) [FOO: ${friend.some_foo}}]"
              )
            }
          }
        })
      ))
    })
  }

  def loadUsers(): Unit = {
    Post[AdminAPI].allUsers().call().map { results =>
      users() = results
    }
    Post[AdminAPI].allFriendsHACK().call().map { results =>
      friends() = results
    }
  }

  def main(): Unit = {
    setupSocket()
    loadUsers()
    dom.document.getElementById("main-content").appendChild(div(
      makeFriendsForm,
      usersTag
    ).render)
  }
}
