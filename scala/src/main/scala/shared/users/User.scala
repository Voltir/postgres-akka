package shared.users

case class UserId(id: Long)

case class User(
  uid: UserId,
  gamertag: String
)