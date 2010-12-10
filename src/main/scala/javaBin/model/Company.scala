package javaBin.model

import net.liftweb.mapper._

object Company extends Company with LongKeyedMetaMapper[Company] with CRUDify[Long, Company]

class Company extends LongKeyedMapper[Company] with IdPK with OneToMany[Long, Company] {
  def getSingleton = Company
  object name extends MappedString(this, 255)
  object address extends MappedString(this, 255)
  object hideMembers extends MappedBoolean(this) {
    override def defaultValue = false
  }
  object contactPerson extends LongMappedMapper(this, Person) {
    override def dbColumnName = "contact_person_id"
  }
  object paidMemberships extends MappedOneToMany(Membership, Membership.companyPaid)
}