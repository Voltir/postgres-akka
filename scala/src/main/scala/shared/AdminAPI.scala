package shared

import shared.users.{UserId, User}

import scala.concurrent.Future

trait AdminAPI {
  def allUsers(): List[User]
  def makeFriends(a: UserId, b: UserId): Unit
  def makeUnfriends(a: UserId, b: UserId): Unit
  def allFriendsHACK(): Map[UserId,List[UserId]]
}

