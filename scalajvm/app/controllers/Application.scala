package controllers

import actors.PGListenActor
import play.api.db.slick.DBAction
import play.api.mvc._
import akka.actor._
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import shared.SharedMessages

object Application extends Controller {
  import play.api.Play.current
  import models.current._
  val lolwat = Akka.system.actorOf(Props[PGListenActor], name=PGListenActor.name)

  def index = Action {
    Ok(views.html.index(SharedMessages.itWorks))
  }

  def create(gamertag: String) = DBAction {  implicit rs =>
    implicit val stab = rs.dbSession
    val user = dao.users.create(gamertag,"just created")
    Ok(dao.userclocks.byId(user.uid).toString())
  }

}
