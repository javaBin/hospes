package javaBin.snippet

import net.liftweb.util._
import Helpers._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import net.liftweb.http.js.JsCmds
import net.liftweb.widgets.autocomplete.AutoComplete
import javaBin.model.{Membership, Person}
import net.liftweb.http.{S, SHtml}
import xml.NodeSeq
import net.liftweb.mapper.{MappedEmail, By, Like}

class Memberships {
  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  def submitMember(email: String, errorFieldId: String, membership: Membership): Unit = {
    val person = Person.find(By(Person.email, email)).openOr{
      val person = Person.create
      person.email.set(email)
      person
    }
    println("Deesa onea: " + person)
    if (person.hasActiveMembership) {
      S.error(errorFieldId, "User " + email + " already have membership")
    } else if (!MappedEmail.validEmailAddr_?(email)) {
      S.error(errorFieldId, "Invalid email address " + email)
    } else {
      person.save
      membership.member.set(person.id)
      membership.save
    }
  }

  def findMatches(current: String, limit: Int): Seq[String] = {
    println("Limit: " + limit)
    if (current.size < 3)
      List[String]()
    else
      Person.findAll(Like(Person.email, current + "%")).filter(!_.hasActiveMembership).take(limit).map(_.email.get)
  }

  private def bindForm(membership: Membership) = {
    val emailForm = <b:email/>
            <span b:errorId=" " class="field_error">
              &nbsp;
            </span>;
    val originalEmail = membership.member.obj.map(_.email.get).openOr("")
    val errorFieldId = Helpers.nextFuncName
    val autocompleteField = AutoComplete(originalEmail, findMatches(_, _), submitMember(_, errorFieldId, membership))
    bind("b", emailForm,
      "email" -> autocompleteField,
      AttrBindParam("errorId", errorFieldId, "id"))
  }

  def render(template: NodeSeq): NodeSeq = {
    Person.currentUser.map{
      user =>
        user.thisYearsBoughtMemberships.foldRight(NodeSeq.Empty) {
          (membership, nodeSeq) =>
            bind("membership", template,
              "boughtDate" -> dateTimeFormatter.print(new DateTime(membership.boughtDate.get)),
              "emailForm" -> SHtml.ajaxForm(bindForm(membership), JsCmds.Noop)
            ) ++ nodeSeq
        }
    }.openOr(NodeSeq.Empty)
  }

}
