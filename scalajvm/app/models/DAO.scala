package models

import models.users.{UserLogicalClockComponent, UserComponent}
import play.api.db.slick._
import scala.slick.driver.JdbcProfile

class DAO(override val profile: JdbcProfile)
    extends UserComponent
    with UserLogicalClockComponent
    with MappedColumnHelper
    with Profile { }

object current {
  val dao = new DAO(DB(play.api.Play.current).driver)

  object ForSlickTableScan {
    private val x1 = dao.users.query
    private val x2 = dao.userclocks.query
  }
}
