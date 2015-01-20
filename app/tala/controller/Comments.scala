package tala.controller

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

import scala.collection.immutable.VectorBuilder
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import play.api._
import play.api.db.DB
import play.api.libs.json._
import play.api.mvc._
import tala.model.CommentRequest
import tala.model.CommentResult
import tala.util.Contexts
import tala.util.Utils
import tala.util.Sanitization

object Comments extends Controller {
    import play.api.Play.current
    import Contexts.dbExecutionContext
    import CommentResult.writes

    def getAction(statement: Connection => PreparedStatement): Action[AnyContent] =
        Action.async {
            Future {
                DB.withConnection { conn =>
                    val results = statement(conn).executeQuery()

                    val comments = new VectorBuilder[CommentResult]
                    while (results.next()) {
                        val id = results.getLong("id")
                        val parent = results.getLong("parent")
                        val created =
                            Utils.floatToDate(results.getDouble("created"))
                        val modified =
                            Utils.floatToDate(results.getDouble("modified"))
                        val text = results.getString("text")
                        val author = results.getString("author")
                        val emailHash = results.getString("email_hash")
                        val website = results.getString("website")
                        comments += CommentResult(id, parent, created, modified,
                            text, author, emailHash, website)
                    }

                    results.close()

                    Ok(Json.obj("comments" -> comments.result))
                }
            } // use database execution context
        }

    def get(uri: String): Action[AnyContent] = getAction { conn: Connection =>
        uri match {
            case "" => // when no uri is provided, get the X latest comments
                val stmt = conn.prepareStatement(
                    """|SELECT
                       |  id, parent, created, modified, text, author,
                       |  email_hash, website
                       |FROM comments
                       |WHERE
                       |  public = 1
                       |ORDER BY
                       |  created DESC
                       |LIMIT 5;""".stripMargin)
                stmt.setQueryTimeout(30)
                stmt
            case _ => // in all other cases, retrieve all comments for that uri
                val stmt = conn.prepareStatement(
                    """|SELECT
                       |  comments.id, parent, created, modified, text,
                       |  author, email_hash, website
                       |FROM threads, comments
                       |WHERE
                       |  threads.uri = ? AND
                       |  threads.id = comments.tid AND
                       |  comments.public = 1
                       |ORDER BY
                       |  comments.created ASC;""".stripMargin)
                stmt.setString(1, uri)
                stmt.setQueryTimeout(30)
                stmt
        }
    }

    def post(uri: String): Action[AnyContent] = Action.async { request =>
        Future {
            request.body.asJson.map {
                case comment: JsObject =>
                    Sanitization.sanitizeComment(comment)
                case _ =>
                    throw new Exception("Invalid JSON structure provided")
            } getOrElse {
                throw new Exception("Expecting content type 'application/json' in request body")
            }
        } flatMap { commentRequest =>
            commentRequest.save(uri)
        } map { commentResult =>
            Created(Json.obj("comment" -> commentResult))
        } recover {
            case e: SQLException =>
                InternalServerError(Json.obj("message" -> s"Internal error: ${e.getMessage}"))
            case e =>
                BadRequest(Json.obj("message" -> s"Invalid request: ${e.getMessage}"))
        }
    }
}
