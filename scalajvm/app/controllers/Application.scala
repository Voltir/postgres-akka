package controllers

import shared.AdminAPI
import shared.users.{UserId, User}
import upickle._

import actors.{TestWebSocketActor, PGListenActor}
import play.api.Routes
import play.api.db.slick.DBAction
import play.api.mvc._
import akka.actor._
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import scala.concurrent.ExecutionContext.Implicits.global

object ImplAdminAPI extends AdminAPI {
  import play.api.Play.current
  import models.current._

  def Session[T](f:  play.api.db.slick.Session => T) = {
    import play.api.Play.current
    play.api.db.slick.DB.withSession(f)
  }

  def allUsers(): List[User] = Session { implicit s =>
    dao.users.all().map(sql => User(sql.uid,sql.gamertag,sql.foo))
  }

  def allFriendsHACK(): Map[UserId,List[UserId]] = Session { implicit s =>
    allUsers().foldLeft(Map[UserId,List[UserId]]()) { case (acc, user) =>
      acc + (user.uid -> dao.usersocial.friends(user.uid))
    }
  }

  def makeFriends(a: UserId, b: UserId): Unit = Session { implicit s =>
    dao.usersocial.request(a,b)
    dao.usersocial.accept(a,b)
  }

  def makeUnfriends(a: UserId, b: UserId): Unit = Session { implicit s =>
    dao.usersocial.remove(a,b)
  }
}

object Application extends Controller with autowire.Server[String,upickle.Reader,upickle.Writer] {
  import play.api.Play.current
  import models.current._
  val lolwat = Akka.system.actorOf(Props[PGListenActor], name=PGListenActor.name)

  def write[Result: Writer](r: Result) = upickle.write(r)

  def read[Result: Reader](p: String) = upickle.read[Result](p)

  def index = Action {
    Ok(views.html.index())
  }

  def create(gamertag: String) = DBAction {  implicit rs =>
    implicit val stab = rs.dbSession
    val user = dao.users.create(gamertag,"just created")
    Ok(dao.userclocks.byId(user.uid).toString())
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    TestWebSocketActor.props(out)
  }

  def autoroute(segment: String) = Action.async(parse.text) { implicit request =>
    Application.route[AdminAPI](ImplAdminAPI)(
      autowire.Core.Request(
        segment.split('/'),
        upickle.read[Map[String,String]](request.body)
      )
    ).map { r =>
      Ok(r).withHeaders("Access-Control-Allow-Origin"->"*")
    }
  }

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(routes.javascript.Application.socket)
    ).as("text/javascript")
  }

}
