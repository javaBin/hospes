package javaBin.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.Empty
import javaBin.model.{MembershipCount, Person, Membership}
import xml.{Text, NodeSeq}

class Admin {

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
      "submit" -> SHtml.ajaxSubmit(S.?("admin.create.memberships.submit"), create))) % ("class" -> "lift-form")
  }

  def showMembers(template: NodeSeq): NodeSeq = {
    val membershipCounts = Membership.countPerYear
    if (membershipCounts.size > 0)
      bind("list", template, "years" -> showMembersPerYear(membershipCounts) _)
    else
      NodeSeq.Empty
  }

  protected def showMembersPerYear(membershipCounts: List[MembershipCount])(template: NodeSeq): NodeSeq =
    membershipCounts.flatMap(membershipCount => listYears(membershipCount)(template))

  protected def listYears(membershipCount: MembershipCount)(template: NodeSeq): NodeSeq = {
    val yearString = membershipCount.year.toString
    bind("members", template,
      "year" -> Text(yearString),
      "validated" -> Text(membershipCount.validatedCount.toString),
      "csvLink" -> <a target="_blank" href={S.hostAndPath + List("rest", "memberships", "year", yearString).mkString("/", "/", "")}>csv</a>)
  }

}
