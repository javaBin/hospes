package javaBin.model

import net.liftweb.mapper._

object Member extends Member with LongKeyedMetaMapper[Member] with CRUDify[Long, Member]

class Member extends LongKeyedMapper[Member] with IdPK {
  def getSingleton = Member
  object companyPaid extends LongMappedMapper(this, Company) {
    override def dbColumnName = "company_paid_id"
  }
}