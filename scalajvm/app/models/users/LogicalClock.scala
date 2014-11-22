package models.users

import models.MappedColumnHelper
import shared.users.UserId

case class UserLogicalClockSQL(
  uid: UserId,
  self: Long,
  relatedUsers: Long,
  gameSessions: Long
)

trait UserLogicalClockComponent {
  self: MappedColumnHelper =>

  import models.GameTimePostgresDriver.simple._

  class UserClocksTable(tag: Tag) extends Table[UserLogicalClockSQL](tag,"notify_akka_user_clocks") {
    def uid = column[UserId]("user_id",O.PrimaryKey)
    def self = column[Long]("self",O.NotNull)
    def relatedUsers = column[Long]("related",O.NotNull)
    def gameSessions = column[Long]("gamez",O.NotNull)
    def * = (uid,self,relatedUsers,gameSessions) <> (UserLogicalClockSQL.tupled,UserLogicalClockSQL.unapply)
  }

   object userclocks {
     val query = TableQuery[UserClocksTable]

     def byId(uid: UserId)(implicit s: Session): Option[UserLogicalClockSQL] = {
       query.filter(_.uid === uid).firstOption
     }
   }
}

