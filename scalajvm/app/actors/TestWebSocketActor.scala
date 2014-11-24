package actors

import actors.PGListenActor.UserOnline
import akka.actor._
import controllers.Application
import upickle._
import shared.users.WebsocketMessages._

import scala.util.{Success => TSuccess, Failure => TFailure, Try}

object TestWebSocketActor {
  def props(out: ActorRef) = Props(new TestWebSocketActor(out))
}

class TestWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String => handleRequest(Try(upickle.read[WSRequestType](msg)))

    case response: WSResponseType => { out ! upickle.write(response) }
  }

  private def handleRequest(msg: Try[WSRequestType]) = {
    msg match {
      case TSuccess(BringOnline(uid)) => {
        val ref = context.actorOf(UserActor.props(uid),UserActor.name(uid))
        Application.lolwat ! UserOnline(uid,ref)
        self ! NowOnline(uid)
      }
      case TSuccess(BringOffline(uid)) => {
        context.child(UserActor.name(uid)).map { ref =>
          ref ! PoisonPill
          self ! NowOffline(uid)
        }
      }
      case TFailure(_) =>  println("Failed to parse WSRequest!")
    }
  }
}