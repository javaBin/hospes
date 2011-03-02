package javaBin.snippet

import javaBin.model.Person
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.util.Helpers._
import scala.xml.NodeSeq

class OpenId {
  def login(template: NodeSeq): NodeSeq = {
    val email = S.param("email").openOr("").trim.toLowerCase

    if (S.post_?) {
      val password = S.param("password").openOr("").trim

      if (email.isEmpty || !MappedEmail.validEmailAddr_?(email)) {
        S.error("emailError", S.?("invalid.email.address", email))
      }

      // If everything is happy clappy, authenticate the user

      println("error# " + S.errors.size)
      if (S.errors.isEmpty) {
        Person.find(By(Person.email, email)) match {
          case Full(user) if !user.validated.is =>
            S.error("Your account is not validated")
          case Full(user) if user.validated.is && user.password.match_?(password) =>
            Person.logUserIn(user)
            S.redirectTo("/openid/retry-login")
          case _ =>
            S.error("Invalid mail or password")
        }
      }
    }

    bind("openid", template,
      "email" -> <input id="email" name="email" value={email}/>
    )
  }
}
