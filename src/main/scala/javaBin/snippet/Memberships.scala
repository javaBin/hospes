package javaBin.snippet

import net.liftweb.util._
import Function.tupled
import Helpers._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import javaBin.model.{Membership, Person}
import net.liftweb.http.{S, SHtml}
import net.liftweb.mapper.{MappedEmail, By}
import net.liftweb.http.js.{JsCmd, JsCmds}
import xml.{Text, NodeSeq}
import net.liftweb.common.Box

class Memberships {
  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  private def submitMember(emailField: => String, errorFieldId: String, infoFieldId: String, membership: Membership, redrawAll: () => JsCmd): JsCmd = {
    var jsCmd = JsCmds.Noop
    val email = emailField
    val person = Person.find(By(Person.email, email)).openOr {
      val person = Person.create
      person.email.set(email)
      person
    }
    if (membership.member.obj.map(_ == person).openOr(false)) {
      // Ignore; same person
    } else if (person.isMemberInActiveMembershipYear) {
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
      person.save()
      membership.member.set(person.id)
      membership.save()
      jsCmd = redrawAll() & jsCmd
    }
    jsCmd
  }

  private def bindForm(membership: Membership, lastYearMembership: Option[Membership], redrawAll: () => JsCmd)(template: NodeSeq) = {
    val errorFieldId = "errorfield" + membership.id
    val infoFieldId = "infofield" + membership.id
    membership.member.obj.filter(_.validated).map {
      person =>
        Text(person.email.is)
    }.openOr {
      val emailSuggestion = Box(lastYearMembership).flatMap(_.member.obj).map(_.email.is).openOr("")
      var currentEmail = membership.member.obj.map(_.email.get).openOr(emailSuggestion)
      val info = for {
        membership <- Box(lastYearMembership)
        member <- membership.member
      } yield S.?("membership.reapply.q", member.mostPresentableName)
      val saveButtonText = membership.member.obj.map(_ => S.?("change")).openOr(S.?("save"))
      SHtml.ajaxForm(bind("form", template,
        "email" -> SHtml.text(currentEmail, currentEmail = _),
        "submit" -> SHtml.ajaxSubmit(saveButtonText, () => submitMember(currentEmail, errorFieldId, infoFieldId, membership, redrawAll)),
        "info" -> info.openOr(""),
        AttrBindParam("errorId", errorFieldId, "id"),
        AttrBindParam("infoId", infoFieldId, "id")))
    }
  }
  
  def bindMemberships(user: Person, lastYearsMemberships: List[Membership], redrawAll: () => JsCmd)(template: NodeSeq): NodeSeq = {
    def pair(ms: List[Membership], lms: List[Membership]): List[(Membership, Option[Membership])] = (ms, lms) match {
      case (Nil, _) => Nil
      case (m :: tail, lym :: lymtail) if (m.member.isEmpty) => (m, Some(lym)) :: pair(tail, lymtail)
      case (m :: tail, lymtail) => (m, None) :: pair(tail, lymtail)
    }
    pair(user.membershipsInActiveYear, lastYearsMemberships).flatMap(tupled {
      (membership, lastYearsMembership) =>
        val status = membership.member.obj
                .map(person => if (person.validated.is) S.?("membership.status.active") else S.?("membership.status.not.validated"))
                .openOr(S.?("membership.status.unassigned"))
        bind("membership", template,
          "boughtDate" -> dateTimeFormatter.print(new DateTime(membership.boughtDate.get)),
          "name" -> Text(membership.member.obj.flatMap(_.nameBox).openOr("-")),
          "status" -> Text(status),
          "form" -> bindForm(membership, lastYearsMembership, redrawAll) _)
    })
  }

  def render(template: NodeSeq): NodeSeq =
    Person.currentUser.map {
      person =>
        val id = Helpers.nextFuncName
        def redrawAll(): JsCmd = JsCmds.Replace(id, render(template))
        val lastYearsMemberships = Membership.lastMemberYearsBoughtMemberships.filter(_.member.obj.map(!_.isMemberInActiveMembershipYear).openOr(false))
        <span id={id}>
          {bind("list", template,
          "memberships" -> bindMemberships(person, lastYearsMemberships, redrawAll) _)}
        </span>
    }.openOr(error("User not available"))

}
