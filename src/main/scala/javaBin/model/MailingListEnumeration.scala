package javaBin.model

import net.liftweb.util.Props

object MailingListEnumeration extends Enumeration {

  def find(listName: String): Option[MailingListEnumeration.Value] =
    values.find(listName == _.toString)

  Props.get("mailing.lists", "").split(",").map(_.trim).filter(!_.isEmpty).map(_.toInt).map {
    index => {
      val propertyName = "mailing.list." + index
      Value(index, Props.get(propertyName).openOr(error("Missing mailing-list with property " + propertyName)))
    }
  }

}
