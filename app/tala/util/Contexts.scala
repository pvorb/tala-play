package tala.util

import scala.concurrent.ExecutionContext

import play.api.libs.concurrent.Akka

object Contexts {
    import play.api.Play.current

    implicit val dbExecutionContext: ExecutionContext =
        Akka.system.dispatchers.lookup("db-context")
}
