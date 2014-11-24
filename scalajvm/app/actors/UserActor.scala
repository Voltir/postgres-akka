package actors

import akka.actor._
import shared.users.UserId

class UserActor(uid: UserId) extends Actor {

  def receive = {
    case _ => "TODO"
  }
}

object UserActor {

  def name(uid: UserId) = s"U${uid.id}"

  def uidOf(path: ActorPath): Option[UserId] = Some(UserId(path.name.drop(1).toLong))

  def props(uid: UserId): Props = Props(new UserActor(uid))
}
