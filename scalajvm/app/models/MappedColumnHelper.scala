package models

import shared.users.UserId

trait MappedColumnHelper {

  import models.GameTimePostgresDriver.simple._

  implicit def UserIdColumn = MappedColumnType.base[UserId,Long](_.id,UserId)
}
