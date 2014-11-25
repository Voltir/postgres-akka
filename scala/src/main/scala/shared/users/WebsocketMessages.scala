package shared.users

object WebsocketMessages {

  //Websocket Request Messages
  sealed trait WSRequestType
  case class BringOnline(uid: UserId) extends WSRequestType
  case class BringOffline(uid: UserId) extends WSRequestType

  //Websocket Response Messages
  sealed trait WSResponseType
  case class NowOnline(uid: UserId) extends WSResponseType
  case class NowOffline(uid: UserId) extends WSResponseType
  case class UserModified(user: User) extends WSResponseType
  case class NowFriends(a: UserId, b: UserId) extends WSResponseType
  case class NowUnfriends(a: UserId, b: UserId) extends WSResponseType
}
