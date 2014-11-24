package actors


import akka.actor._
import shared.users.UserId

import scala.util.Try

class PGListenActor extends Actor with ActorLogging {
  import actors.PGListenActor._

  val IDENTIFIER = "listener1"

  lazy val conn = new PGNotifyConnection(self, IDENTIFIER)

  override def preStart() = {
    println("STARTING LISTENER AKKA!!!")
    conn.start()
  }

  override def postStop = {
    conn.shutdown()
  }

  override def receive = {
    case PGNotified(channel,payload,pid) => {
      // Messages assumed to be in the form of res0:
      // scala> upickle.write(("user_update",Map.empty[String,String]))
      // res0: String = ["user_update",[]]
      println(":::: DEBUG ::::",channel,payload)
      val result = Try(upickle.read[(String,Map[String,String])](payload))
      result match {
        case scala.util.Success((kind,args)) => println("SUCCESS!",kind,args)
        case scala.util.Failure(e) => println("COULD NOT PARSE RESULT!",e)
      }
    }

    case UserOnline(uid,ref) => {
      context.watch(ref)
      conn.online(uid)
    }

    case Terminated(ref) => {
      UserActor.uidOf(ref.path).map { uid =>
        conn.offline(uid)
      }
    }

    case _ => println("PGLISTENACTOR -- ERROR!")
  }
}

object PGListenActor {

  val name = "pg-listener"

  //TODO: Make a supervisor
  case class UserOnline(uid: UserId, ref: ActorRef)

  //Message Types
  case class PGNotified(channel: String, payload: String, pid: Int)

  def props: Props = Props(new PGListenActor)
}

protected class PGNotifyConnection(notifier: ActorRef, channel: String) extends Thread {
  import java.sql.DriverManager
  import org.postgresql.PGConnection

  private[this] var isRunning = true

  val javaConn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/notifyakka", "postgres", "password")
  val psqlConn = javaConn.asInstanceOf[PGConnection]

  def shutdown() = {
    isRunning = false
  }

  def online(uid: UserId) = {
    execute(s"INSERT INTO akka_live_users VALUES ('$channel',${uid.id})")
  }

  def offline(uid: UserId) = {
    execute(s"DELETE FROM akka_live_users WHERE user_id = ${uid.id}")
  }

  private def execute(statement: String) = {
    println(s"EXECUTING $statement")
    val stmt = javaConn.createStatement
    stmt.execute(statement)
    stmt.close()
  }

  override def run() {
   execute(s"LISTEN akka_debug")
    while(isRunning) {
      try {
        val fromPostgres = psqlConn.getNotifications
        if(fromPostgres != null) {
          for(pgnoti <- fromPostgres) {
            println(s"GOT THING! ${pgnoti.getName}: ${pgnoti.getParameter}")
            notifier ! PGListenActor.PGNotified(pgnoti.getName,pgnoti.getParameter,pgnoti.getPID)
          }
        }
      } catch {
        case exception: Exception => println(s"OMG FAILURE: ${exception.getMessage}")
      } finally {
        Thread.sleep(100)
      }
    }
  }
}
