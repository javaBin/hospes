package javaBin.model

import net.liftweb.util.Props

object MailingListEnumeration {

  private val map = Props.get("mailing.lists", "").split(",").map(_.trim).filter(!_.isEmpty).map(_.toInt).map {
    index =>
      val propertyName = "mailing.list." + index
      val name = Props.get(propertyName).openOr(error("Missing mailing-list with property " + propertyName))
      val propertyTitle = "mailing.list." + name
      val title = Props.get(propertyTitle).openOr(error("Missing mailing-list title with property " + propertyTitle))
      (name -> Value(index, name, title))
  } toMap

  def find(listName: String): Option[Value] = map.get(listName)

  def values = map.values

  case class Value(i: Int, name: String, title: String)

}
