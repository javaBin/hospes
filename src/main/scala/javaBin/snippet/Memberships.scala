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

  private def submitMember(emailField: => String, errorFieldId: String, infoFieldId: String, membership: Membership, redrawAll: () => JsCmd): JsCmd = {
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
      jsCmd = redrawAll() & jsCmd
    }
    jsCmd
  }

  private def bindForm(membership: Membership, redrawAll: () => JsCmd)(template: NodeSeq) = {
    val errorFieldId = "errorfield" + membership.id
    val infoFieldId = "infofield" + membership.id
    membership.member.obj.filter(_.validated).map{
      person =>
        Text(person.email.is)
    }.openOr{
      var currentEmail = membership.member.obj.map(_.email.get).openOr("")
      SHtml.ajaxForm(bind("form", template,
        "email" -> SHtml.text(currentEmail, currentEmail = _),
        "submit" -> SHtml.ajaxSubmit(S.?("save"), () => submitMember(currentEmail, errorFieldId, infoFieldId, membership, redrawAll)),
        AttrBindParam("errorId", errorFieldId, "id"),
        AttrBindParam("infoId", infoFieldId, "id")))
    }
  }

  def bindMemberships(user: Person, redrawAll: () => JsCmd)(template: NodeSeq): NodeSeq = user.thisYearsBoughtMemberships.flatMap{
    membership =>
      bind("membership", template,
        "boughtDate" -> dateTimeFormatter.print(new DateTime(membership.boughtDate.get)),
        "form" -> bindForm(membership, redrawAll) _)
  }

  def render(template: NodeSeq): NodeSeq = {
    val id = Helpers.nextFuncName
    def redrawAll(): JsCmd = JsCmds.Replace(id, render(template))
    Person.currentUser.map{
      person =>
        <span id={id}>{
          bind("list", template,
          "memberships" -> bindMemberships(person, redrawAll) _)}
        </span>
    }.openOr(error("User not available"))
  }

}
