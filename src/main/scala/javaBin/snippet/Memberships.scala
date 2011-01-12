package javaBin.snippet

import net.liftweb.util._
import Helpers._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import javaBin.model.{Membership, Person}
import net.liftweb.http.{S, SHtml}
import net.liftweb.mapper.{MappedEmail, By}
import net.liftweb.http.js.{JsCmd, JsCmds}
import xml.{Text, NodeSeq}

class Memberships {
  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  private def submitMember(emailField: => String, errorFieldId: String, infoFieldId: String, membership: Membership): JsCmd = {
    var jsCmd = JsCmds.Noop
    val email = emailField
    val person = Person.find(By(Person.email, email)).openOr{
      val person = Person.create
      person.email.set(email)
      person
    }
    if (person.hasActiveMembership) {
      S.error(errorFieldId, S.?("has.active.membership", email))
    } else if (!MappedEmail.validEmailAddr_?(email)) {
      S.error(errorFieldId, S.?("invalid.email.address", email))
    } else {
      val buyer = Person.currentUser.open_!
      if (!person.saved_?) {
        jsCmd = jsCmd & JsCmds.SetHtml(infoFieldId, Text(S.?("created.new.member")))
        person.sendNewMemberConfirmationEmail(buyer)
      } else {
        jsCmd = jsCmd & JsCmds.SetHtml(infoFieldId, Text(S.?("added.to.member")))
        person.sendMembershipRenewedConfirmationEmail(buyer)
      }
      person.save
      membership.member.set(person.id)
      membership.save
      jsCmd = jsCmd & JsCmds.SetHtml(errorFieldId, Text(""))
    }
    jsCmd
  }

  private def bindForm(membership: Membership, emailForm: NodeSeq) = {
    var currentEmail = membership.member.obj.map(_.email.get).openOr("")
    val errorFieldId = Helpers.nextFuncName
    val infoFieldId = Helpers.nextFuncName
    bind("b", emailForm,
      "email" -> SHtml.text(currentEmail, currentEmail = _),
      "submit" -> SHtml.ajaxSubmit(S.?("save"), () => submitMember(currentEmail, errorFieldId, infoFieldId, membership)),
      AttrBindParam("errorId", errorFieldId, "id"),
      AttrBindParam("infoId", infoFieldId, "id"))
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
