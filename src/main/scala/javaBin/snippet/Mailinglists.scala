package javaBin.snippet

import net.liftweb.util.Helpers
import Helpers._
import xml.{Text, NodeSeq}
import net.liftweb.http.js.{JsCmd, JsCmds}
import javaBin.model.Person
import net.liftweb.http.{S, SHtml}

class MailingLists {

  private def bindMailingLists(person: Person, redrawAll: () => JsCmd)(template: NodeSeq): NodeSeq =
    person.mailingLists.sortBy(_.mailingList.is).flatMap {
      mailingListSubscription =>
        val mailingListName = S.?("mailing.list." + mailingListSubscription.name)
        def toggle(set: Boolean): JsCmd = {
          person.mailingList(mailingListSubscription.mailingList.is).checked(set).save()
          val msg = if (set) "mailing.list.added" else "mailing.list.removed"
          S.notice(S.?(msg, mailingListName))
          redrawAll() & JsCmds.Noop
        }
        bind("mailingList", template,
          "toggle" -> (SHtml.ajaxCheckbox(mailingListSubscription.checked.is, toggle) ++ Text(mailingListName)))
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
    }.openOr(error("User not available"))
  }

}