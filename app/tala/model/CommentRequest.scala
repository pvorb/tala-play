package tala.model

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try
import play.api.db.DB
import tala.util.Utils
import scala.compat.Platform
import java.sql.SQLException
import scala.util.Success
import scala.util.Failure
import java.util.Date

case class CommentRequest(parent: Long, text: String, threadTitle: String,
                          author: String, emailHash: String, website: String) {
    import play.api.Play.current

    def save(uri: String)(implicit ctx: ExecutionContext): Future[CommentResult] =
        Future {
            DB.withConnection { conn =>
                // add the corresponding thread
                val createThread = conn.prepareStatement(
                    """|INSERT INTO threads
                       |  (uri, title)
                       |VALUES
                       |  (?, ?);""".stripMargin)
                createThread.setString(1, uri)
                createThread.setString(2, threadTitle)

                // get the id of the inserted or old thread
                val tid = try {
                    createThread.execute() // this might throw a SQLException
                    val keys = createThread.getGeneratedKeys
                    keys.next()
                    // if a new entry was made, use the new id
                    keys.getLong("id")
                } catch {
                    case e: SQLException =>
                        // otherwise do a SELECT to get the id of the thread
                        val getThreadId = conn.prepareStatement(
                            "SELECT id FROM threads WHERE uri = ?;")
                        getThreadId.setString(1, uri)
                        val result = getThreadId.executeQuery()
                        if (!result.next()) {
                            throw new Exception("unexpected")
                        }
                        result.getLong("id")
                }

                // create the new comment
                val createComment = conn.prepareStatement(
                    """|INSERT INTO comments
                       |  (tid, parent, created, modified, public, text, author,
                       |    email_hash, website)
                       |VALUES
                       |  (?, ?, ?, ?, ?, ?, ?, ?, ?);""".stripMargin)

                val now = Platform.currentTime
                val nowFloat = Utils.dateToFloat(now)
                createComment.setLong(1, tid)
                createComment.setLong(2, parent)
                createComment.setDouble(3, nowFloat)
                createComment.setDouble(4, nowFloat)
                createComment.setInt(5, 1)
                createComment.setString(6, text)
                createComment.setString(7, author)
                createComment.setString(8, emailHash)
                createComment.setString(9, website)

                // get the id of the inserted comment
                createComment.execute() // this might throw a SQLException
                val keys = createComment.getGeneratedKeys()
                keys.next()
                // if a new entry was made, get the new id
                val id = keys.getLong(1)

                val nowDate = new Date(now)
                CommentResult(id, parent, created = nowDate, modified = nowDate,
                    text, author, emailHash, website)
            }
        }
}
