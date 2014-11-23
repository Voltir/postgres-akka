package models

import scala.reflect.ClassTag
import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType


trait PgUpickleSupport extends com.github.tminglei.slickpg.json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import upickle._

  trait UpickleJsonImplicits[Upickle] {
    implicit def upickleTypeMapper(implicit reader: Reader[Upickle], writer: Writer[Upickle], workplz: ClassTag[Upickle]) =
      new GenericJdbcType[Upickle]("json",
        (v) => upickle.read[Upickle](v),
        (v) => upickle.write[Upickle](v),
        hasLiteralForm = false
      )

    implicit def upickleColumnExtensionMethods(c: Column[Upickle])(
      implicit tm: JdbcType[Upickle], tm1: JdbcType[List[String]], reader: Reader[Upickle], writer: Writer[Upickle]) = {
      new JsonColumnExtensionMethods[Upickle, Upickle](c)
    }
    implicit def upickleOptionColumnExtensionMethods(c: Column[Option[Upickle]])(
      implicit tm: JdbcType[Upickle], tm1: JdbcType[List[String]], reader: Reader[Upickle], writer: Writer[Upickle]) = {
      new JsonColumnExtensionMethods[Upickle, Option[Upickle]](c)
    }
  }
}

trait NotifyTestPostgresDriver extends PostgresDriver
    with PgEnumSupport
    with PgArraySupport
    with PgDateSupport
    with PgRangeSupport
    with PgHStoreSupport
    with PgSearchSupport
    with PgUpickleSupport {

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus{}

  trait ImplicitsPlus extends Implicits
  with ArrayImplicits
  with DateTimeImplicits
  with RangeImplicits
  with HStoreImplicits
  with SearchImplicits
  //with UpickleJsonImplicits[NotificationData]

  trait SimpleQLPlus extends SimpleQL
  with ImplicitsPlus
  with SearchAssistants
}

object GameTimePostgresDriver extends NotifyTestPostgresDriver
