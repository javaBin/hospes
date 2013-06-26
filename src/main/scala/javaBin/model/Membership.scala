package javaBin.model

import net.liftweb.mapper._
import java.util.Date
import org.joda.time.DateTime
import net.liftweb.util.Props
import net.liftweb.http.S

object Membership extends Membership with LongKeyedMetaMapper[Membership] with CRUDify[Long, Membership] {

  def path = "memberships"
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

  def search(year: Int, email: String): List[Membership] = {
    val emailPattern = "%" + email + "%"
    Membership.findAllByPreparedStatement({ superConnection =>
      val statement = superConnection.connection.prepareStatement("""
        select m.* from Membership m
        left outer join Person pm on pm.id = m.member_person_id
        left outer join Person pb on pb.id = m.bought_by_person_id
        where m.year = ? and
        (pm.email like ? or pm.firstName like ? or pm.lastName like ?
        or pb.email like ? or pb.firstName like ? or pb.lastName like ?)""")
      statement.setInt(1, year)
      statement.setString(2, emailPattern)
      statement.setString(3, emailPattern)
      statement.setString(4, emailPattern)
      statement.setString(5, emailPattern)
      statement.setString(6, emailPattern)
      statement.setString(7, emailPattern)
      statement
    })
  }

}

class Membership extends LongKeyedMapper[Membership] with IdPK {
  def getSingleton = Membership
  object year extends MappedInt(this) {
    override def defaultValue = currentYear
  }
  object member extends MappedLongForeignKey(this, Person) {
    override def dbColumnName = "member_person_id"
  }
  object boughtDate extends MappedDateTime(this) {
    override def defaultValue = new Date
  }
  object boughtBy extends MappedLongForeignKey(this, Person) {
    override def dbColumnName = "bought_by_person_id"
  }
  def isForCurrentYear = year == currentYear
  def status = member.obj.map {
    person => if (person.validated.is) S.?("membership.status.active") else S.?("membership.status.not.validated")
  }.openOr(S.?("membership.status.unassigned"))
  private def currentYear = (new DateTime).getYear
}
