package models.users

import models.MappedColumnHelper
import shared.users.UserId

sealed trait RelationSQL
case object Friends extends RelationSQL
case object ARequestedB extends RelationSQL
case object BRequestedA extends RelationSQL
case object ErrorRelation extends RelationSQL

case class SocialSQL(
  a: UserId,
  b: UserId,
  r: RelationSQL
)

trait SocialComponent {
  self: MappedColumnHelper =>

  import models.GameTimePostgresDriver.simple._

  class SocialTable(tag: Tag) extends Table[SocialSQL](tag,"notify_akka_social") {
    def a = column[UserId]("uid_a",O.NotNull)
    def b = column[UserId]("uid_b",O.NotNull)
    def r = column[RelationSQL]("relation",O.NotNull)
    def * = (a,b,r) <> (SocialSQL.tupled, SocialSQL.unapply)
    def aIndex = index("social_a",a)
    def bIndex = index("social_b",b)
  }

  object usersocial {
    val query = TableQuery[SocialTable]

    private def order(a: UserId, b: UserId): (UserId,UserId) = {
      if(a.id < b.id) (a,b) else (b,a)
    }

    def request(requester: UserId, requestee: UserId)(implicit s: Session): Option[SocialSQL] = {
      val (a,b) = order(requester,requestee)
      if(query.filter(r => r.a === a && r.b === b).exists.run) None
      else {
        val rel: RelationSQL =
          if( b == requester) BRequestedA
          else ARequestedB
        val result = (query returning query) += SocialSQL(a,b,rel)
        Option(result)
      }
    }

    def accept(requester: UserId, requestee: UserId)(implicit s: Session): Int = {
      val (a,b) = order(requester,requestee)
      val rel: RelationSQL =
        if( b == requester) BRequestedA
        else ARequestedB
      query
        .filter(r => r.a === a && r.b === b && r.r === rel)
        .map(_.r)
        .update(Friends)
    }

    def reject(requester: UserId, requestee: UserId)(implicit s: Session): Int = {
      val (a,b) = order(requester,requestee)
      val rel: RelationSQL =
        if( b == requester) BRequestedA
        else ARequestedB
      query
        .filter(r => r.a === a && r.b === b && r.r === rel)
        .delete
    }

    def remove(u1: UserId, u2: UserId)(implicit s: Session): Int = {
      val (a,b) = order(u1,u2)
      query
        .filter(r => r.a === a && r.b === b && r.r === (Friends:RelationSQL))
        .delete
    }

    def pending(uid: UserId)(implicit s: Session): List[UserId] = {
      val q = query
        .filter(r => r.a === uid && r.r === (BRequestedA:RelationSQL)).map(_.b)
        .union(
          query.filter(r => r.b === uid && r.r === (ARequestedB:RelationSQL)).map(_.a)
        )
      q.list
    }

    def requested(uid: UserId)(implicit s: Session): List[UserId] = {
      val q = query
        .filter(r => r.a === uid && r.r === (ARequestedB:RelationSQL)).map(_.b)
        .union(
          query.filter(r => r.b === uid && r.r === (BRequestedA:RelationSQL)).map(_.a)
        )
      q.list
    }

    def friends(uid: UserId)(implicit s: Session): List[UserId] = {
      val q = query
        .filter(r => r.a === uid && r.r === (Friends:RelationSQL)).map(_.b)
        .union(
          query.filter(r => r.b === uid && r.r === (Friends:RelationSQL)).map(_.a)
        )
      q.list
    }
  }
}