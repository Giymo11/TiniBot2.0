package science.wasabi.tini.web


import java.io.File

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze.BlazeBuilder

import scalaz.concurrent.Task


object ServerMain extends ServerApp {

  val helloWorldService = HttpService {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

  val service = HttpService {
    case request @ GET -> Root / "index.html" =>
      StaticFile.fromFile(new File("./index.html"), Some(request))
        .map(Task.now) // This one is require to make the types match up
        .getOrElse(NotFound()) // In case the file doesn't exist
  }

  override def server(args: List[String]): Task[Server] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(helloWorldService, "/api")
      .mountService(service)
      .start
  }
}
