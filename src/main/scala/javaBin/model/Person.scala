package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.sitemap.Loc.Hidden
import javaBin.util.XMLUtil._
import net.liftweb.http.S

object Person extends Person with MetaMegaProtoUser[Person] {
  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber, password)
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)

  def errorMsgXhtml = <lift:msgs showAll="true"><lift:error_class>field_error</lift:error_class></lift:msgs>
  override def loginXhtml = super.loginXhtml.addChild(errorMsgXhtml)
  override def lostPasswordXhtml = super.lostPasswordXhtml.addChild(errorMsgXhtml)
  override def editXhtml(user: Person) = super.editXhtml(user).addChild(errorMsgXhtml)
  override def changePasswordXhtml = super.changePasswordXhtml.addChild(errorMsgXhtml)
  override def signupXhtml(user: Person) = super.signupXhtml(user).addChild(errorMsgXhtml)

  override def signupMailSubject = S.?("sign.up.confirmation")

  override def lostPasswordMenuLocParams = Hidden :: super.lostPasswordMenuLocParams
  override lazy val sitemap = List(loginMenuLoc, createUserMenuLoc, lostPasswordMenuLoc, editUserMenuLoc, changePasswordMenuLoc, validateUserMenuLoc, resetPasswordMenuLoc).flatten(a => a)
}

class Person extends MegaProtoUser[Person] with OneToMany[Long, Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = S.?("phone.number")
  }
  object address extends MappedText(this) {
    override def displayName = S.?("address")
  }
  object memberships extends MappedOneToMany(Membership, Membership.member)
  object boughtMemberships extends MappedOneToMany(Membership, Membership.boughtBy)
  @deprecated("Company is out?")
  object employer extends LongMappedMapper(this, Company) {
    override def dbColumnName = "employer_company_id"
  }
  @deprecated("Company is out?")
  object isContactPerson extends MappedBoolean(this) {
    override def defaultValue = false
  }
  def name = Seq(firstName, lastName).mkString(" ")
  def thisYearsBoughtMemberships = boughtMemberships.filter(_.isCurrent)
  def hasActiveMembership = memberships.exists(_.isCurrent)
}
