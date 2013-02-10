package javaBin.snippet

import xml.{NodeSeq, Text}
import javaBin.model.Person
import net.liftweb.http.S

class UserInformation {

  def current(xhtml: NodeSeq): NodeSeq =
    Person.currentUser.map {
      user => Text(user.niceName)
    }.openOr {
      Text(S.?("user.not.logged.in"))
    }

  def hasMembership(xhtml: NodeSeq): NodeSeq =
    Person.currentUser.map {
      user => Text(if (user.isMember) S.?("membership.has") else S.?("membership.has.not"))
    }.openOr(NodeSeq.Empty)


  def button(xhtml: NodeSeq): NodeSeq =
    Person.currentUser.map {
      user =>
        <a href={Person.logoutPath.mkString("/", "/", "")}>
          <lift:loc.logout/>
        </a>
    }.openOr {
      <a href={Person.loginPath.mkString("/", "/", "")}>
        <lift:loc.login/>
      </a>
    }

}
