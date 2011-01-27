package javaBin.snippet

import xml.{NodeSeq, Text}
import javaBin.model.Person
import net.liftweb.http.S

class UserInformation {

  def current(xhtml: NodeSeq): NodeSeq =
    Text(Person.currentUser.map(_.niceName).openOr(S.?("user.not.logged.in")))

}