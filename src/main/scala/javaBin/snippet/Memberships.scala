package javaBin.snippet

import net.liftweb.util._
import Helpers._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import net.liftweb.http.SHtml
import xml.NodeSeq
import net.liftweb.http.js.JsCmds
import net.liftweb.widgets.autocomplete.AutoComplete
import net.liftweb.mapper.{By, Like}
import javaBin.model.{Membership, Person}

class Memberships {
  private lazy val dateTimeFormat = new DateTimeFormatterBuilder().append(ISODateTimeFormat.date).appendLiteral(' ').append(ISODateTimeFormat.hourMinute)
  lazy val dateTimeFormatter = dateTimeFormat.toFormatter

  val emailForm: NodeSeq = <b:email/>
            <b:submit/>;

  private def bindForm(membership: Membership) = {
    val email = membership.member.obj.map(_.email.get).openOr("")
    var emailSet = email;
    bind("b", emailForm,
      "email" -> AutoComplete(email, {
        (current, limit) =>
          println("Limit: " + limit)
          if (current.size < 3)
            List[String]()
          else
            Person.findAll(Like(Person.email, current + "%")).filter(!_.hasActiveMembership).map(_.email.get).take(limit)
      }, emailSet = _),
      "submit" -> SHtml.ajaxSubmit("Save", {
        () =>
          val person = Person.find(By(Person.email, emailSet)).openOr {
            val person = Person.create
            person.email.set(emailSet)
            person
          }
          println("Deesa onea: " + person)
          if (person.hasActiveMembership)
            println("Shit")
          else {
            person.save
            membership.member.set(person.id)
            membership.save
          }
          JsCmds.Noop
      }))
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
