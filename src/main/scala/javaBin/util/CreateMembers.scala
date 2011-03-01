package javaBin.util

import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import dispatch.Http
import Http._

object CreateMembers {
  def createJson(email: String = "person1@lainternet.com", itemAmount: Int = 6, omitEmail: Boolean = false) = {
    ("user" ->
      ("firstname" -> "Johnny B. Æøå") ~
      ("lastname" -> "Goode") ~
      (if (!omitEmail) ("email" -> email) else ("t" -> "t"))) ~
    ("items" -> List(("Tull", -5), ("Goody", itemAmount), ("Fjas", 1003)).map{
      item =>
        ("name" -> item._1) ~
        ("amount" -> item._2)
    }) ~
    ("reference" -> "ref1")
  }

  def main(args: Array[String]): Unit = {
    val json = createJson(args(0))
    val jsonContentTypes = Map("Content-Type" -> "application/json; charset=utf-8", "Accept" -> "application/json")
    Http x ("http://localhost:8090/rest/memberships" << pretty(render(json)) <:< jsonContentTypes as_! ("webshop", "webschopp") >|) {
      case (200, _, _, _) => println("Success")
      case (status, _, _, _) => println("Shit: " + status)
    }
  }
}
