package javaBin.snippet

import javaBin.model.{MailingListEnumeration, Person}
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds
import net.liftweb.util.Helpers
import Helpers._
import xml.{Text, NodeSeq}

class MailingLists {

  private def bindMailingLists(person: Person)(template: NodeSeq): NodeSeq =
    MailingListEnumeration.values.toList.sortBy(_.id).flatMap {
      mailingList =>
        bind("mailingList", template,
          "toggle" -> (SHtml.ajaxCheckbox(false, _ => JsCmds.Noop) ++ Text(mailingList.toString)))
    }

  def render(template: NodeSeq): NodeSeq = {
    Person.currentUser.map {
      person =>
        bind("list", template,
          "mailingList" -> bindMailingLists(person) _)
    }.openOr(error("User not available"))
  }

}