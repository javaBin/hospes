package javaBin

import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import dispatch.Http
import Http._

object CreateMembers {
  def createJson(email: String = "person1@lainternet.com", itemAmount: Int = 6, omitEmail: Boolean = false) = {
    ("user" ->
      ("firstname" -> "Johnny B.") ~
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
    val json = createJson()
    Http x ("http://localhost:8080/memberships" << pretty(render(json)) <:< Map("Content-Type" -> "application/json", "Accept" -> "application/json") >|) {
      case (200, _, _, _) => println("Success")
      case (status, _, _, _) => println("Shit: " + status)
    }
  }
}