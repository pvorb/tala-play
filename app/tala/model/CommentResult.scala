package tala.model

import java.util.Date
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Json

case class CommentResult(id: Long, parent: Long, created: Date, modified: Date,
                         text: String, author: String, emailHash: String, website: String)

object CommentResult {
    implicit val writes = Json.writes[CommentResult]
}
