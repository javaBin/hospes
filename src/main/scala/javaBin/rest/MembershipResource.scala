package javaBin
package rest

import model.{Membership, Person}

import net.liftweb.common._
import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.json.{JsonParser, JsonAST, JsonDSL}
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.mapper.{MappedEmail, By}

import dispatch.Http

object MembershipResource extends BetterRestHelper with Extractors {

  serve {
    case JsonPost("rest" :: "memberships" :: Nil, (json, _)) => createMembership(json)

    case JsonGet("rest" :: "memberships" :: email :: Nil, req) =>
      val realmail = Http -% (email + (if (req.path.suffix.isEmpty) "" else "." + req.path.suffix))
      (for{
        person <- Person.find(By(Person.email, realmail)) if person.hasActiveMembership && person.validated
      } yield JsonResponse(asJson(person))).or(Full(NotFoundResponse()))

    case JsonGet("rest" :: "memberships" :: Nil, _) =>
      val members = Person.findAll.filter(person => person.hasActiveMembership && person.validated)
      JsonResponse("members" -> JArray(members.map(asJson)))
  }

  def asJson(person:Person):JValue =
    ("firstname" -> person.firstName.is) ~
    ("lastname" -> person.lastName.is) ~
    ("email" -> person.email.is)

  def createMembership(req: JsonAST.JValue): LiftResponse = {

    val people =
      for {
        JField("user", JObject(user)) <- req
        JField("email", JString(ValidEmailAddress(email))) <- user
        JField("items", JArray(items)) <- req
        item <- items
        JField("amount", JInt(NotMoreThanThousand(NonZero(Positive(amount))))) <- item
      } yield {
        val person = Person.create
        person.email.set(email)
        person.firstName.set((req \ "user" \ "firstname").extractOpt[String].getOrElse(""))
        person.lastName.set((req \ "user" \ "lastname").extractOpt[String].getOrElse(""))
        (person, amount)
      }

    people.foldRight[LiftResponse](ExplicitBadResponse("No memberships found")) {
      (pair, _) =>
        val (temporaryPerson, amount) = pair
        val person = Person.find(By(Person.email, temporaryPerson.email)).map{
          person =>
            person.sendMembershipsReceivedEmail
            person
        }.getOrElse{
          temporaryPerson.validated(true).save
          temporaryPerson.sendMembershipsReceivedAndUserCreateEmail
          temporaryPerson
        }
        val i = amount.intValue
        Membership.createMany(i, person)
        OkResponse()
    }
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
}
