package javaBin.model

import net.liftweb.mapper._

object Membership extends Membership with LongKeyedMetaMapper[Membership] with CRUDify[Long, Membership]

class Membership extends LongKeyedMapper[Membership] with IdPK {
  def getSingleton = Membership
  object year extends MappedInt(this)
  object person extends LongMappedMapper(this, Person) {
    override def dbColumnName = "person_id"
  }
  object companyPaid extends LongMappedMapper(this, Company) {
    override def dbColumnName = "company_paid_id"
  }
  def isExpired = year != 2010
}