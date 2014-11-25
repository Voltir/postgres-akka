package shared.users

case class UserId(id: Long)

case class User(
  uid: UserId,
  gamertag: String,
  some_foo: String
)