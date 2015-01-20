package tala.util

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

import scala.util.Try

object Utils {
    val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val isoDateFormat: DateFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    private val uriDateFormat: DateFormat =
        new SimpleDateFormat("yyyyMMdd")

    def dateToIso8601(date: Date): String = isoDateFormat.format(date)
    def parseIso8601(date: String): Try[Date] = Try(isoDateFormat.parse(date))

    def dateToUriFormat(date: Date): String = uriDateFormat.format(date)
    def parseUriDate(date: String): Try[Date] = Try(uriDateFormat.parse(date))

    private val md5inst = MessageDigest.getInstance("MD5")
    def md5(msg: String): String = {
        val hashBytes = md5inst.digest(msg.getBytes(StandardCharsets.UTF_8))
        val hashInt = new BigInteger(1, hashBytes)
        String.format("%0"+(hashBytes.length << 1)+"x", hashInt)
    }

    private val sha1inst = MessageDigest.getInstance("SHA-1")
    def sha1(msg: String): String = {
        val hashBytes = sha1inst.digest(msg.getBytes(StandardCharsets.UTF_8))
        val hashInt = new BigInteger(1, hashBytes)
        String.format("%0"+(hashBytes.length << 1)+"X", hashInt)
    }

    def floatToDate(date: Double): Date = new Date(date.toLong * 1000L)
    def dateToFloat(date: Date): Double = date.getTime / 1000d
    def dateToFloat(date: Long): Double = date / 1000d
}
