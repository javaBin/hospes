package javaBin.snippet

import xml.{NodeSeq, Text}
import javaBin.model.Person
import net.liftweb.http.{SHtml, S}

class UserInformation {

  def current(xhtml: NodeSeq): NodeSeq =
    Person.currentUser.map {
      user =>
        Text(user.niceName) ++
        <br/><a href={Person.logoutPath.mkString("/", "/", "")}><lift:loc.logout/></a>
    }.openOr{
      Text(S.?("user.not.logged.in")) ++
      <br/><a href={Person.loginPath.mkString("/", "/", "")}><lift:loc.login/></a>
    }

}