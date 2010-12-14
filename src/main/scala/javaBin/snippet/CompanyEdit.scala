package javaBin.snippet

import net.liftweb.util._
import Helpers._
import javaBin.model.{Company, Person}
import net.liftweb.http.{SHtml, S}
import xml.{Text, NodeSeq}
import net.liftweb.http.js.JsCmds

/**
 * @author Thor Åge Eldby (thoraageeldby@gmail.com)
 */

class CompanyEdit {
  def edit(template: NodeSeq): NodeSeq = {

    val xml = for {
      user <- Person.currentUser
      company <- Company.find(user.employer)
    } yield bind("company", template,
        "name" -> company.name.toForm,
        "address" -> company.address.toForm,
        "hideMembers" -> company.hideMembers.toForm,
        "submit" -> SHtml.submit("Update", () => company.save))
    xml openOr NodeSeq.Empty
  }

  def contactPeople(template: NodeSeq): NodeSeq = {
    val xml = for {
      user <- Person.currentUser
      company <- Company.find(user.employer)
    } yield company.employees.toList.foldRight(NodeSeq.Empty)((person, nodeSeq) =>
        bind("person", template,
          "name" -> new Text(person.name),
          "toggleContact" -> SHtml.ajaxCheckbox(
            person.isContactPerson.get, {
            enabled =>
              person.isContactPerson.set(enabled)
              person.save
              JsCmds.Noop
            }))
                ++ nodeSeq
      )
    xml openOr NodeSeq.Empty
  }

}