package javaBin.snippet

import xml.NodeSeq
import net.liftweb.util.Helpers._
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds
import javaBin.model.{Person, Membership}
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.Empty

object CreateMemberships {
  def path = "create_memberships"
}

class CreateMemberships {

  def createMemberships(template: NodeSeq): NodeSeq = {
    var email = ""
    var count = ""
    val emailField = SHtml.text(email, email = _) % ("id" -> "email")
    val countField = SHtml.text(count, count = _) % ("id" -> "count")
    def create() = {
      var jsCmd = JsCmds.Noop
      val emailErrorId = "emailError"
      val countErrorId = "countError"
      email = email.trim.toLowerCase
      count = count.trim
      if (email.isEmpty || !MappedEmail.validEmailAddr_?(email)) {
        S.error(emailErrorId, S.?("invalid.email.address", email))
      } else if (Person.find(By(Person.email, email)) == Empty) {
        S.error(emailErrorId, S.?("email.address.unknown", email))
      } else {
        jsCmd = jsCmd & JsCmds.Replace(emailErrorId, NodeSeq.Empty)
      }
      if (!count.matches("[0-9]+")) {
        S.error(countErrorId, S.?("not.a.positive.number"))
      } else {
        jsCmd = jsCmd & JsCmds.Replace(countErrorId, NodeSeq.Empty)
      }
      if (S.errors.isEmpty) {
        Membership.createMany(count.toInt, Person.find(By(Person.email, email)).openOr(sys.error("User " + email + " not found")))
        S.notice(S.?("admin.created.memberships", count.toInt, email))
        jsCmd = jsCmd & JsCmds.Replace("email", emailField) & JsCmds.Replace("count", countField)
      }
      jsCmd
    }
    SHtml.ajaxForm(bind("create", template,
      "email" -> emailField,
      "count" -> countField,
      "submit" -> SHtml.ajaxSubmit(S.?("admin.create.memberships.submit"), create) % ("class" -> "btn")
    )) % ("class" -> "form-horizontal")
  }

}
