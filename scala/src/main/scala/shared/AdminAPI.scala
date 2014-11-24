package shared

import shared.users.User

import scala.concurrent.Future

trait AdminAPI {
  def allUsers(): List[User]
}

