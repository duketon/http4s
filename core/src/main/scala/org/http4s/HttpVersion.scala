package org.http4s

import scala.language.experimental.macros
import scala.math.Ordered.orderingToOrdered
import scalaz.{\/-, Order, Show, -\/}
import scalaz.\/._

import org.http4s.parser.{ScalazDeliverySchemes, Rfc2616BasicRules}
import org.http4s.util.{Renderable, Writer}
import org.parboiled2._

/**
 * An HTTP version, as seen on the start line of an HTTP request or response.
 *
 * @see [http://tools.ietf.org/html/rfc7230#section-2.6 RFC 7320, Section 2.6
 */
final case class HttpVersion private[HttpVersion] (major: Int, minor: Int) extends Renderable with Ordered[HttpVersion] {
  override def render(writer: Writer): writer.type = writer << "HTTP/" << major << '.' << minor
  override def compare(that: HttpVersion): Int = (this.major, this.minor) compare ((that.major, that.minor))
}

object HttpVersion extends HttpVersionInstances {
  val `HTTP/1.0` = new HttpVersion(1, 0)
  val `HTTP/1.1` = new HttpVersion(1, 1)
  val `HTTP/2.0` = new HttpVersion(2, 0)

  def fromString(s: String): ParseResult[HttpVersion] = s match {
    case "HTTP/1.1" => right(`HTTP/1.1`)
    case "HTTP/1.0" => right(`HTTP/1.0`)
    case other => new Parser(s).HttpVersion.run()(ScalazDeliverySchemes.Disjunction).leftMap { _ =>
      ParseFailure("Invalid HTTP version", s"$s was not found to be a valid HTTP version")
    }
  }

  private class Parser(val input: ParserInput) extends org.parboiled2.Parser with Rfc2616BasicRules {
    def HttpVersion: Rule1[org.http4s.HttpVersion] = rule {
      "HTTP/" ~ capture(Digit) ~ "." ~ capture(Digit) ~> { (major: String, minor: String) => new HttpVersion(major.toInt, minor.toInt) }
    }
  }

  def fromVersion(major: Int, minor: Int): ParseResult[HttpVersion] = {
    if (major < 0) ParseResult.fail("Invalid HTTP version", s"major must be > 0: $major")
    else if (major > 9) ParseResult.fail("Invalid HTTP version", s"major must be <= 9: $major")
    else if (minor < 0) ParseResult.fail("Invalid HTTP version", s"major must be > 0: $minor")
    else if (minor > 9) ParseResult.fail("Invalid HTTP version", s"major must be <= 9: $minor")
    else ParseResult.success(new HttpVersion(major, minor))
  }
}

trait HttpVersionInstances {
  implicit val HttpVersionShow = Show.showFromToString[HttpVersion]
  implicit val HttpVersionOrder = Order.fromScalaOrdering[HttpVersion]
}
