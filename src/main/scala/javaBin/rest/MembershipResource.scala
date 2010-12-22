package javaBin.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._
import net.liftweb.mapper.By
import javaBin.model.{Membership, Person}

object MembershipResource extends RestHelper {
  serve{
    case JsonPost("memberships" :: Nil, (json, _)) => createMembership(json)
  }

  object Positive {
    def unapply[T <: Number](value: T): Option[T] = if (value.doubleValue >= 0.0) Some(value) else None
  }
  class Limited(limit: Int) {
    def unapply[T <: Number](value: T): Option[T] = if (value.doubleValue < limit) Some(value) else None
  }
  object NotMoreThanThousand extends Limited(1000)

  def createMembership(req: JsonAST.JValue): LiftResponse = {
    println("Request: " + req)
    val people =
      for (JField("user", JObject(user)) <- req;
           JField("email", JString(email)) <- user;
           JField("items", JArray(items)) <- req;
           item <- items;
           JField("amount", JInt(NotMoreThanThousand(Positive(amount)))) <- item)
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
        val person = Person.find(By(Person.email, temporaryPerson.email)).map {
          person =>
            // TODO: Send epost
            println("bruk gammel person")
            person
        }.getOrElse({
          // TODO: Send epost
          println("lager ny person")
          temporaryPerson.password("passord").validated(true).save
          temporaryPerson
        })
        (0 until amount.intValue).foreach { _ =>
          val membership = Membership.create
          membership.boughtBy.set(person.id)
          membership.save
        }
        println("all is ok")
        OkResponse()
    }
  }

  case class ExplicitBadResponse(description: String) extends LiftResponse with HeaderDefaults {
    def toResponse = InMemoryResponse(description.getBytes, headers, cookies, 400)
  }

}