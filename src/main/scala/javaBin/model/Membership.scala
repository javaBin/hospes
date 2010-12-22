package javaBin.model

import net.liftweb.mapper._
import java.util.Date
import org.joda.time.DateTime

object Membership extends Membership with LongKeyedMetaMapper[Membership] with CRUDify[Long, Membership]

class Membership extends LongKeyedMapper[Membership] with IdPK {
  def getSingleton = Membership
  object year extends MappedInt(this) {
    override def defaultValue = currentYear
  }
  object member extends LongMappedMapper(this, Person) {
    override def dbColumnName = "member_person_id"
  }
  @deprecated("Company is out?")
  object companyPaid extends LongMappedMapper(this, Company) {
    override def dbColumnName = "company_paid_id"
  }
  object boughtDate extends MappedDateTime(this) {
    override def defaultValue = new Date
  }
  object boughtBy extends LongMappedMapper(this, Person) {
    override def dbColumnName = "bought_by_person_id"
  }
  def isCurrent = year == currentYear
  def currentYear = (new DateTime).getYear
}
