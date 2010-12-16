package javaBin.snippet

import xml.NodeSeq
import net.liftweb.util._
import Helpers._
import javaBin.model.Company

/**
 * @author Thor Åge Eldby (thoraageeldby@gmail.com)
 */

class MemberList {
  def companies(companyTemplate: NodeSeq): NodeSeq =
    Company.findAll.filter{_.paidMemberships.exists(_.isCurrent)}.flatMap {
      company =>
        def bindPersons(personTemplate: NodeSeq): NodeSeq =
          if (company.hideMembers)
            NodeSeq.Empty
          else
            company.employees.flatMap(person => bind("person", personTemplate, "name" -> person.name))
        bind("company", companyTemplate, "name" -> company.name, "persons" -> bindPersons _)
    }
}
