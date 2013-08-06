package javaBin.snippet

import scala.xml.{Text, NodeSeq}
import net.liftweb.http.{S, SHtml}
import net.liftweb.util.Helpers
import Helpers._
import net.liftweb.http.js.{JE, JsCmd, JsCmds}
import javaBin.model.Membership
import net.liftweb.common.{Empty, Full}
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder}

object MembershipsSearch {
  val path = "memberships_search"
}

class MembershipsSearch {
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

  def render(template: NodeSeq): NodeSeq = render(template, Form(None, "", Nil))

  def render(template: NodeSeq, form: Form): NodeSeq = {
    val resultId = Helpers.nextFuncName
    val replaceResults = (form: Form) => JsCmds.Replace(resultId, render(template, form))
    def verifyChangesOnSelection(modalName: String, f: Membership => Unit) = {
      form.selected.foreach(f)
      JE.JsRaw("$('" + modalName + "').modal('toggle')").cmd &
        JsCmds.Replace(resultId, render(template, form.load))
    }
    <span id={resultId}>{
      bind("search", template,
        "deleteSelected" -> SHtml.ajaxButton(S.?("admin.memberships.delete"),
          () => verifyChangesOnSelection("#delete-modal", _.delete_!)) %
          ("class" -> "btn btn-danger"),
        "unapplySelected" -> SHtml.ajaxButton(S.?("admin.memberships.unapply"),
          () => verifyChangesOnSelection("#unapply-modal", _.member(Empty).save())) %
          ("class" -> "btn btn-danger"),
        "form" -> renderForm(form, replaceResults) _,
        "count" -> Text(form.results.size.toString),
        "countApplied" -> Text(form.results.count(_.member.isDefined).toString),
        "results" -> renderResults(form) _)
    }</span>
  }

  case class Form(year: Option[Int], email: String, results: List[Membership]) {
    var selected: Set[Membership] = Set()
    def load = year.map(y => copy(results = Membership.search(y, email))).getOrElse(this)
  }

}
