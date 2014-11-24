package models.users

import models.MappedColumnHelper
import shared.users._

case class UserSQL(
  uid: UserId,
  gamertag: String,
  foo: String
)

trait UserComponent {
    self: MappedColumnHelper =>

  import models.GameTimePostgresDriver.simple._

  class UsersTable(tag: Tag) extends Table[UserSQL](tag,"notify_akka_users") {
    def uid = column[UserId]("user_id",O.PrimaryKey,O.AutoInc)
    def gamertag = column[String]("gamertag",O.NotNull)
    def foo = column[String]("some_foo",O.NotNull)
    def * = (uid,gamertag,foo) <> (UserSQL.tupled,UserSQL.unapply)
  }

  object users {
    val query = TableQuery[UsersTable]

    def create(gamertag: String, foo: String)(implicit s: Session): UserSQL = {
      (query returning query) += UserSQL(UserId(-1),gamertag,foo)
    }

    def all()(implicit s: Session): List[UserSQL] = {
      query.list
    }

    def modifyFoo(uid: UserId, foo: String)(implicit s: Session): Int = {
      query.filter(_.uid === uid).map(_.foo).update(foo)
    }
  }
}