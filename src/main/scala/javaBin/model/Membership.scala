package javaBin.model

import net.liftweb.mapper._
import java.util.Date
import org.joda.time.DateTime

object Membership extends Membership with LongKeyedMetaMapper[Membership] with CRUDify[Long, Membership] {
  def membershipsPath = "memberships"
  def adminPath = "admin"

  def createMany(i: Int, person: Person): Unit = {
    (0 until i).foreach{
      _ =>
        val membership = Membership.create
        membership.boughtBy.set(person.id)
        membership.save
    }
  }
}

class Membership extends LongKeyedMapper[Membership] with IdPK {
  def getSingleton = Membership
  object year extends MappedInt(this) {
    override def defaultValue = currentYear
  }
  object member extends LongMappedMapper(this, Person) {
    override def dbColumnName = "member_person_id"
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
