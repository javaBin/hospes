package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._

object Person extends Person with MetaMegaProtoUser[Person] {
  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber)
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)
  override def skipEmailValidation = true
}

class Person extends MegaProtoUser[Person] with OneToMany[Long, Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = "Phone number"
  }
  object address extends MappedText(this) {
    override def displayName = "Address"
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
