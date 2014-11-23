package shared.users

object Social {
  object Relation extends Enumeration {
    type Relation = Value
    val Self = Value(0)
    val Pending = Value(1)
    val Requested = Value(2)
    val Friends = Value(3)
    val Stranger = Value(4)
  }
}
