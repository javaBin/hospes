package javaBin
package rest

import model.{Membership, Person}

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.mapper.By

import dispatch.Http

object MembershipResource extends BetterRestHelper with Extractors {

  serve {
    case JsonPost("rest" :: "memberships" :: Nil, (json, _)) => createMembership(json)

    case JsonGet("rest" :: "memberships" :: email :: Nil, req) =>
      val realMail = Http -% (email + (if (req.path.suffix.isEmpty) "" else "." + req.path.suffix))
      (for{
        person <- Person.find(By(Person.email, realMail)) if person.hasActiveMembership && person.validated
      } yield JsonResponse(asJson(person))).or(Full(NotFoundResponse()))

    case JsonGet("rest" :: "memberships" :: Nil, _) =>
      val members = Person.findAll().filter(person => person.hasActiveMembership && person.validated)
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

