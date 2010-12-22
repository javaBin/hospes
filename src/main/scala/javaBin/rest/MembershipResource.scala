package javaBin.rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.http._
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._

object MembershipResource extends RestHelper {
  serve {
    case JsonPost("memberships" :: Nil, (json, _)) => createMembership(json)
  }

  def createMembership(req: JsonAST.JValue): LiftResponse = {
    (req \ "user" \ "email").extractOpt[String].flatMap { email =>
      (req \ "items" \ "amount").extractOpt[JArray].map { items =>
        println("items: " + items)
        items.map { item =>
          println("item: " + item)
          val s = "email: " + (item \ "name") + ", items: " + (item \ "amount")
          println(s)
          item
        }
        OkResponse()
      }
    }.getOrElse(ExplicitBadResponse("Unable to find email address"))
  }

  case class ExplicitBadResponse(description: String) extends LiftResponse with HeaderDefaults {
    def toResponse = InMemoryResponse(description.getBytes, headers, cookies, 400)
  }
}