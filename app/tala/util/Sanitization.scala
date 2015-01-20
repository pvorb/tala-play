package tala.util

import java.net.URL

import play.api.libs.json.JsObject

import org.owasp.encoder.Encode
import org.owasp.html.HtmlPolicyBuilder

import tala.model.CommentRequest

object Sanitization {
    val textPolicy = new HtmlPolicyBuilder().allowElements("br", "p", "pre",
        "code", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "hr", "a", "abbr", "em", "strong", "img")
        .allowAttributes("title").globally()
        .allowAttributes("href").onElements("a")
        .allowAttributes("src", "alt", "width", "height").onElements("img")
        .allowAttributes("lang").globally()
        .allowStandardUrlProtocols()
        .toFactory()

    def sanitizeComment(obj: JsObject): CommentRequest = {
        val parent = (obj \ "parent").as[Long]
        assert(parent >= -1L, s"Parent must be an integer >= -1, was $parent")

        // sanitize text with text policy
        val text = textPolicy.sanitize((obj \ "text").as[String])

        val threadTitle =
            Encode.forHtmlContent((obj \ "threadTitle").as[String])

        val author = Encode.forHtmlContent((obj \ "author").as[String])
        assert(author == null || (author.length > 0 && author.length <= 64),
            s"author must be a non-empty string, but it should not be longer than 64, was $author")

        val emailHash = (obj \ "emailHash").as[String]
        assert(emailHash == null || emailHash.length == 32,
            s"emailHash must be a string of 32 hexadecimal characters, was '$emailHash'")

        val websiteStr = (obj \ "website").as[String]
        val website = try {
            new URL(websiteStr).toString()
        } catch {
            case _: Throwable => try {
                new URL("http://"+websiteStr).toString()
            } catch {
                case _: Throwable => null
            }
        }

        CommentRequest(parent, text, threadTitle, author, emailHash, website)
    }
}
