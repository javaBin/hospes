package javaBin.snippet

import net.liftweb.util._
import Helpers._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import javaBin.model.{Membership, Person}
import net.liftweb.http.{S, SHtml}
import net.liftweb.mapper.{MappedEmail, By, Like}
import net.liftweb.http.js.{JsCmd, JsCmds}
import xml.{Text, NodeSeq}

class Memberships {
  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  private def submitMember(emailField: => String, errorFieldId: String, membership: Membership): JsCmd = {
    var jsCmd = JsCmds.Noop
    val email = emailField
    val person = Person.find(By(Person.email, email)).openOr{
      val person = Person.create
      person.email.set(email)
      person
    }
    if (person.hasActiveMembership) {
      S.error(errorFieldId, "User " + email + " already have membership")
    } else if (!MappedEmail.validEmailAddr_?(email)) {
      S.error(errorFieldId, "Invalid email address " + email)
    } else {
      person.save
      membership.member.set(person.id)
      membership.save
      jsCmd = JsCmds.SetHtml(errorFieldId, Text(""))
    }
    jsCmd
  }

  private def bindForm(membership: Membership, emailForm: NodeSeq) = {
    var currentEmail = membership.member.obj.map(_.email.get).openOr("")
    val errorFieldId = Helpers.nextFuncName
    bind("b", emailForm,
      "email" -> SHtml.text(currentEmail, currentEmail = _),
      "submit" -> SHtml.ajaxSubmit("Save", () => submitMember(currentEmail, errorFieldId, membership)),
      AttrBindParam("errorId", errorFieldId, "id"))
  }

  def render(template: NodeSeq): NodeSeq = {
    Person.currentUser.map{
      user =>
        user.thisYearsBoughtMemberships.foldRight(NodeSeq.Empty) {
          (membership, nodeSeq) =>
            bind("membership", template,
              "boughtDate" -> dateTimeFormatter.print(new DateTime(membership.boughtDate.get)),
              "emailForm" -> SHtml.ajaxForm(bindForm(membership, chooseTemplate("membership", "emailForm", template)), JsCmds.Noop)
            ) ++ nodeSeq
        }
    }.openOr(NodeSeq.Empty)
  }

}
