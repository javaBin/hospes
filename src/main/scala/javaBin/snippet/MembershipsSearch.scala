package javaBin.snippet

import scala.xml.{Text, NodeSeq}
import net.liftweb.http.{S, SHtml}
import net.liftweb.util.Helpers
import Helpers._
import net.liftweb.http.js.{JE, JsCmd, JsCmds}
import javaBin.model.{Person, Membership}
import net.liftweb.common.{Logger, Empty, Full}
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder}
import net.liftweb.mapper.By

object MembershipsSearch {
  val path = "memberships_search"
}

class MembershipsSearch extends Logger {

  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  def renderForm(form: Form, replaceResults: Form => JsCmd)(template: NodeSeq): NodeSeq = {
    val activeYear = Membership.activeMembershipYear
    var year = form.year.getOrElse(activeYear).toString
    var email = form.email
    val years = (activeYear - 5) to activeYear
    val func = () => {
      val y = Integer.valueOf(year)
      replaceResults(Form(year = Some(y), email = email, Nil).load)
    }
    SHtml.ajaxForm(bind("search", template,
      "years" -> SHtml.select(years.map(_.toString).map(a => (a, a)).toSeq, Full(year), year = _) % ("class" -> "span2"),
      "email" -> SHtml.text(email, email = _) % ("class" -> "span3"),
      "submit" -> SHtml.ajaxSubmit(S.?("admin.memberships.search"), func) % ("class" -> "btn")
    )) % ("class" -> "form-horizontal")
  }

  def renderResults(form: Form)(template: NodeSeq): NodeSeq =
    form.results.flatMap { membership =>
      bind("membership", template,
        "boughtByEmail" -> Text(membership.boughtBy.obj.map(_.niceName).getOrElse("")),
        "memberEmail" -> Text(membership.member.obj.map(_.niceName).getOrElse("")),
        "year" -> Text(membership.year.is.toString),
        "boughtDate" -> dateTimeFormatter.print(new DateTime(membership.boughtDate.get)),
        "status" -> Text(membership.status),
        "select" -> SHtml.ajaxCheckbox(form.selected.contains(membership), value => {
          if (value)
            form.selected += membership
          else
            form.selected -= membership
          JsCmds.Noop
        }))
    }

  def moveForm(resultId: String, allTemplate: NodeSeq, form: Form)(template: NodeSeq): NodeSeq = {
    val messageId = Helpers.nextFuncName
    var email = ""
    def setEmail(value: String): JsCmd = {
      email = value.trim.toLowerCase
      JsCmds.Noop
    }
    def submit(): JsCmd = {
      Person.find(By(Person.email, email)).map { person =>
        form.verifyChangesOnSelection(resultId, allTemplate, "#move-modal", membership => {
          val details = membership.toString()
          val boughtBy = membership.boughtBy.obj
          membership.boughtBy(person).save()
          audit("Membership moved: " + details + ", from " + boughtBy.map(_.niceName).getOrElse("none")
            + " to " + membership.boughtBy.map(_.niceName).getOrElse("none"))
        })
      }.getOrElse {
        JsCmds.Replace(messageId, renderMessage(S.?("email.address.unknown", email)))
      }
    }
    def renderMessage(msg: String) = <span id={messageId}>{msg}</span>
    SHtml.ajaxForm(bind("form", template,
      "message" -> renderMessage(""),
      "email" -> SHtml.ajaxText(email, setEmail) % ("placeholder" -> S.?("admin.memberships.move.email.placeholder")),
      "moveSelected" -> SHtml.ajaxSubmit(S.?("admin.memberships.move"), submit) % ("class" -> "btn btn-danger")
    )) % ("class" -> "form-horizontal")
  }

  def render(template: NodeSeq): NodeSeq = render(template, Form(None, "", Nil))

  def render(template: NodeSeq, form: Form): NodeSeq = {
    val resultId = Helpers.nextFuncName
    val replaceResults = (form: Form) => JsCmds.Replace(resultId, render(template, form))
    def deleteMembership(membership: Membership) = {
      val details = membership.toString()
      membership.delete_!
      audit("Membership deleted: " + details)
    }
    def unapplyMembership(membership: Membership) = {
      val details = membership.toString()
      membership.member(Empty).save()
      audit("Membership unapplied: " + details)
    }
    <span id={resultId}>{
      bind("search", template,
        "moveForm" -> moveForm(resultId, template, form) _,
        "deleteSelected" -> SHtml.ajaxButton(S.?("admin.memberships.delete"),
          () => form.verifyChangesOnSelection(resultId, template, "#delete-modal", deleteMembership)) %
          ("class" -> "btn btn-danger"),
        "unapplySelected" -> SHtml.ajaxButton(S.?("admin.memberships.unapply"),
          () => form.verifyChangesOnSelection(resultId, template, "#unapply-modal", unapplyMembership)) %
          ("class" -> "btn btn-danger"),
        "form" -> renderForm(form, replaceResults) _,
        "count" -> Text(form.results.size.toString),
        "countApplied" -> Text(form.results.count(_.member.isDefined).toString),
        "results" -> renderResults(form) _)
    }</span>
  }


  def audit(message: => String) {
    info(Person.currentUser.map(_.niceName).getOrElse("unknown") + " -> " + message)
  }

  def jsBootstrapModalClose(modalName: String): JsCmd = {
    JE.JsRaw("$('" + modalName + "').modal('toggle')").cmd
  }

  case class Form(year: Option[Int], email: String, results: List[Membership]) {
    var selected: Set[Membership] = Set()
    def load = year.map(y => copy(results = Membership.search(y, email))).getOrElse(this)
    def verifyChangesOnSelection(resultId: String, template: NodeSeq, modalName: String, f: Membership => Unit) = {
      selected.foreach(f)
      jsBootstrapModalClose(modalName) & JsCmds.Replace(resultId, render(template, load))
    }
  }

}
