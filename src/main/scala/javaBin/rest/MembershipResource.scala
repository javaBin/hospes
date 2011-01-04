package javaBin.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._
import javaBin.model.{Membership, Person}
import net.liftweb.mapper.{MappedEmail, By}

object MembershipResource extends RestHelper {
  serve {
    case JsonPost("rest" :: "memberships" :: Nil, (json, _)) => createMembership(json)
  }

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

  def createMembership(req: JsonAST.JValue): LiftResponse = {
    val people =
      for (JField("user", JObject(user)) <- req;
           JField("email", JString(ValidEmailAddress(email))) <- user;
           JField("items", JArray(items)) <- req;
           item <- items;
           JField("amount", JInt(NotMoreThanThousand(NonZero(Positive(amount))))) <- item)
      yield {
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
            person.sendSubscriptionsReceivedEmail
            person
        }.getOrElse{
          temporaryPerson.validated(true).save
          temporaryPerson.sendSubscriptionsReceivedAndUserCreateEmail
          temporaryPerson
        }
        (0 until amount.intValue).foreach{
          _ =>
            val membership = Membership.create
            membership.boughtBy.set(person.id)
            membership.save
        }
        OkResponse()
    }
  }

  case class ExplicitBadResponse(description: String) extends LiftResponse with HeaderDefaults {
    def toResponse = InMemoryResponse(description.getBytes, headers, cookies, 400)
  }

}