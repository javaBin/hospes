package javaBin.snippet

import net.liftweb.http.SHtml
import net.liftweb.util.Helpers
import Helpers._
import xml.{Text, NodeSeq}
import net.liftweb.http.js.{JsCmd, JsCmds}
import javaBin.model.{MailingListSubscription, MailingListEnumeration, Person}

class MailingLists {

  private def bindMailingLists(person: Person)(template: NodeSeq): NodeSeq =
    person.mailingLists.sortBy(_.mailingList.is).flatMap {
      mailingList =>
        def toggle(set: Boolean): JsCmd = {
          person.mailingList(mailingList.mailingList.is).checked(set).save()
          JsCmds.Noop
        }
        bind("mailingList", template,
          "toggle" -> (SHtml.ajaxCheckbox(mailingList.checked.is, toggle) ++ Text(mailingList.name)))
    }

  def render(template: NodeSeq): NodeSeq = {
    Person.currentUser.map {
      person =>
        bind("list", template,
          "mailingList" -> bindMailingLists(person) _)
    }.openOr(error("User not available"))
  }

}