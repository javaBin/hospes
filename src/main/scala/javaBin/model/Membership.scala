package javaBin.model

import net.liftweb.mapper._
import java.util.Date
import org.joda.time.DateTime
import net.liftweb.util.Props

object Membership extends Membership with LongKeyedMetaMapper[Membership] with CRUDify[Long, Membership] {

  def membershipsPath = "memberships"
  def adminPath = "admin"
  def activeMembershipYear = Props.getInt("membership.year") openOr new DateTime().getYear

  def createMany(i: Int, person: Person) {
    (0 until i).foreach{
      _ =>
        val membership = Membership.create
        membership.boughtBy.set(person.id)
        membership.year.set(activeMembershipYear)
        membership.save()
    }
  }
  
  def lastMemberYearsBoughtMemberships: List[Membership] =
    Person.currentUser.map{
      currentUser => Membership.findAll(By(boughtBy, currentUser.id), By(year, activeMembershipYear - 1))
    }.openOr(Nil)
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
  def isForCurrentYear = year == currentYear
  private def currentYear = (new DateTime).getYear
}
