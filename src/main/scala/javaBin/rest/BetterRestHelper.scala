package javaBin.rest

import net.liftweb.mapper.MappedEmail
import net.liftweb.http.rest.RestHelper
import net.liftweb.json.{JsonParser, JsonAST}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.common.{Full, Failure, Empty, Box}
import net.liftweb.http.provider.HTTPCookie
import net.liftweb.http._

trait BetterRestHelper extends RestHelper {
  // TODO: Hacked to get right encoding
  def json(req: Req): Box[JsonAST.JValue] =
    try {
      import _root_.java.io._
      req.body.map(b => JsonParser.parse(new InputStreamReader(new ByteArrayInputStream(b), org.apache.http.protocol.HTTP.UTF_8)))
    } catch {
      case e: Exception => Failure(e.getMessage, Full(e), Empty)
    }

  protected trait RealJsonBody extends JsonBody {
    override def body(r: Req): Box[JValue] = json(r)
  }

  override protected lazy val JsonPost = new TestPost[JValue] with JsonTest with RealJsonBody

  lazy val CsvGet = new TestGet with CsvTest

  protected trait CsvTest {
    def testResponse_?(r: Req): Boolean =
      r.weightedAccept.find(_.matches("text" -> "json")).isDefined ||
              (r.weightedAccept.isEmpty && r.path.suffix.equalsIgnoreCase("csv"))
  }

}

object CsvResponse {
  def apply(content: List[List[String]]) = new CsvResponse(content, S.getHeaders(Nil), S.responseCookies, 200)
}

case class CsvResponse(content: List[List[String]], headers: List[(String, String)], cookies: List[HTTPCookie], code: Int) extends LiftResponse {
  def toResponse = {
    val bytes = content.map(_.reduceLeft(_ + "," + _)).foldLeft(new StringBuilder())((s,l) => s.append(l + "\n")).toString.getBytes("UTF-8")
    InMemoryResponse(bytes, ("Content-Length", bytes.length.toString) :: ("Content-Type", "text/csv; charset=utf-8") :: headers, cookies, code)
  }
}

trait Extractors {

  object Positive {
    def unapply[T <: Number](value: T): Option[T] = if (value.doubleValue >= 0.0) Some(value) else None
  }

  object NonZero {
    def unapply[T <: Number](value: T): Option[T] = if (value.doubleValue != 0.0) Some(value) else None
  }

  class Limited(limit: Int) {
    def unapply[T <: Number](value: T): Option[T] = if (value.doubleValue < limit) Some(value) else None
  }

  object NotMoreThanThousand extends Limited(1000)

  object ValidEmailAddress {
    def unapply(value: String): Option[String] = if (MappedEmail.validEmailAddr_?(value)) Some(value) else None
  }

}

case class ExplicitBadResponse(description: String) extends LiftResponse with HeaderDefaults {
  def toResponse = InMemoryResponse(description.getBytes, headers, cookies, 400)
}
