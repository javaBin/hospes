package javaBin.snippet

import net.liftweb.util.Helpers
import Helpers._
import xml.{Text, NodeSeq}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.{S, SHtml}
import javaBin.model.{MailingListEnumeration, Person}

class MailingLists {

  private def bindMailingLists(person: Person, redrawAll: () => JsCmd)(template: NodeSeq): NodeSeq =
    person.mailingLists.sortBy(_.mailingList.is).flatMap {
      mailingListSubscription =>
        val mailingListName = mailingListSubscription.mailingList.is
        val mailingListTitle = MailingListEnumeration.find(mailingListName).map(_.title).getOrElse(mailingListName)
        def toggle(set: Boolean): JsCmd = {
          person.mailingList(mailingListSubscription.mailingList.is).checked(set).save()
          val msg = if (set) "mailing.list.added" else "mailing.list.removed"
          S.notice(S.?(msg, mailingListTitle))
          redrawAll() & JsCmds.Noop
        }
        bind("mailingList", template,
          "toggle" -> (SHtml.ajaxCheckbox(mailingListSubscription.checked.is, toggle) ++ Text(mailingListTitle)))
    }

  def render(template: NodeSeq): NodeSeq = {
    Person.currentUser.map {
      person =>
        val id = Helpers.nextFuncName
        def redrawAll(): JsCmd = JsCmds.Replace(id, render(template))
        <span id={id}>{
          bind("list", template,
            "mailingList" -> bindMailingLists(person, redrawAll) _)
        }</span>
    }.openOr(sys.error("User not available"))
  }

}