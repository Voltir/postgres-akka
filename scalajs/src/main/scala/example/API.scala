package example

import upickle._
import org.scalajs.dom
import scala.concurrent.{Promise, Future}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object API {
  //import fuckit._

  lazy val secure: Boolean = false

  def post(url: String, pickled: String, retry: Boolean = true): Future[String] = {
    dom.extensions.Ajax.post(
      url = url,
      data = pickled
    ).map(_.responseText)
  }


  object Post extends autowire.Client[String,upickle.Reader,upickle.Writer] {

    override def write[Result: Writer](r: Result) = upickle.write(r)

    override def read[Result: Reader](p: String) = upickle.read[Result](p)

    override def doCall(req: Request): Future[String] = {
      val url = "/autoroute/" + req.path.mkString("/")
      API.post(url,upickle.write(req.args))
    }
  }
}