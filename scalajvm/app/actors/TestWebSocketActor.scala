package actors

import actors.PGListenActor.{PGDebugEvent, UserOnline}
import akka.actor._
import controllers.Application
import shared.users.{UserId, User}
import upickle._
import shared.users.WebsocketMessages._

import scala.util.{Success => TSuccess, Failure => TFailure, Try}

object TestWebSocketActor {
  def props(out: ActorRef) = Props(new TestWebSocketActor(out))
}

class TestWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String => handleRequest(Try(upickle.read[WSRequestType](msg)))

    case PGDebugEvent(args) => {
      println("\n\nTODO HANDLE DEBUG EVENT")
      println(args)
      args.get("debug_kind") match {
        case Some("user_modified") => {
          for {
            uid <- args.get("user_id").map(_.toLong)
            gamertag <- args.get("gamertag")
            foo <- args.get("some_foo")
          } yield {
            self ! UserModified(User(UserId(uid),gamertag,foo))
          }
        }
        case Some("social_modified") => {
          for {
            uid_a <- args.get("uid_a").map(_.toLong)
            uid_b <- args.get("uid_b").map(_.toLong)
            relation <- args.get("relation").map(_.toLong)
            lclock_a <- args.get("lclock_a").map(_.toLong)
            lclock_b <- args.get("lclock_b").map(_.toLong)
          } yield {
            self ! NowFriends(UserId(uid_a),UserId(uid_b))
          }
        }
        case _ => {
          println("UNHANDLED DEBUG KIND!")
        }
      }
    }

    case response: WSResponseType => { out ! upickle.write(response) }
  }

  override def preStart() = {
    Application.lolwat ! PGListenActor.DebugRef(self)
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