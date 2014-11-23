package models

import models.users._
import shared.users.UserId

trait MappedColumnHelper {

  import models.GameTimePostgresDriver.simple._

  implicit def UserIdColumn = MappedColumnType.base[UserId,Long](_.id,UserId)

  private def relationToInt(r: RelationSQL): Int = r match {
    case Friends => 1
    case ARequestedB => 2
    case BRequestedA => 3
    case ErrorRelation => 99
  }

  private def intToRelation(r: Int): RelationSQL = r match {
    case 1 => Friends
    case 2 => ARequestedB
    case 3 => BRequestedA
    case _ => ErrorRelation
  }

  implicit def RelationColumn = MappedColumnType.base[RelationSQL,Int](relationToInt,intToRelation)
}
