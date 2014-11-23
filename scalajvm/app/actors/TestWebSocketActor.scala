package actors

import akka.actor._

object TestWebSocketActor {
  def props(out: ActorRef) = Props(new TestWebSocketActor(out))
}

class TestWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      println("YAR!",msg)
      out ! ("I received your message: " + msg)
  }
}