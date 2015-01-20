package tala.controller

import scala.concurrent.Future

import play.api._
import play.api.db.DB
import play.api.libs.json._
import play.api.mvc._

import tala.util.Contexts

object CommentCounts extends Controller {
    import play.api.Play.current

    def get(uri: String) = Action.async {
        Future {
            val count: Long = DB.withConnection { conn =>
                val stmt = if (uri != "") {
                    val stmt = conn.prepareStatement(
                        """|SELECT COUNT(*)
                           |FROM threads, comments
                           |WHERE
                           |  threads.uri = ? AND
                           |  threads.id = comments.tid;
                           |""".stripMargin)
                    stmt.setString(1, uri)
                    stmt
                } else {
                    val stmt = conn.prepareStatement(
                        """|SELECT COUNT(*)
                           |FROM comments;
                           |""".stripMargin)
                    stmt
                }
                stmt.setQueryTimeout(30)
                val results = stmt.executeQuery()

                // get the count from the result
                val count =
                    if (results.next())
                        results.getLong(1)
                    else
                        -1L // invalid value

                results.close()

                count
            }

            if (count >= 0L) {
                Ok(Json.obj("count" -> count))
            } else {
                InternalServerError(Json.obj("error" ->
                    s"Unknown exception for uri = '$uri'"))
            }
        }(Contexts.dbExecutionContext)
    }

}
