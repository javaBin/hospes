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
    (0 until i).foreach {
      _ =>
        val membership = Membership.create
        membership.boughtBy.set(person.id)
        membership.year.set(activeMembershipYear)
        membership.save()
    }
  }

  object AnInt {
    def unapply(s: String): Option[Int] = try {
      Some(s.toInt)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  def countPerYear: List[MembershipCount] = {
    val yearColumn = year.dbColumnName
    val (_, resultList) = DB.runQuery(
      " select m." + yearColumn + " year, count(*) validatedCount" +
              " from " + dbTableName + " m, " + Person.dbTableName + " p" +
              " where p." + Person.validated.dbColumnName + " = true" +
              " and p." + Person.id.dbColumnName + " = m." + this.member.dbColumnName +
              " group by m." + yearColumn)
    for (member <- Membership.findAll())
      println("results: " + member)
    for (yearRow <- resultList) yield yearRow match {
      case List(AnInt(year), AnInt(validatedCount)) => MembershipCount(year, validatedCount)
      case _ => error("Unexpected output " + yearRow)
    }
  }

  def memberReportForYear(year: Int) = {
    val (head, resultList) = DB.runQuery(" select " +
            List(
              List(id, this.year, member, boughtDate, boughtBy).map(_.dbColumnName).mkString("m.", ", m.", ""),
              List(Person.id, Person.email, Person.firstName, Person.lastName, Person.address, Person.phoneNumber).map(_.dbColumnName).mkString("p.", ", p.", "")
            ).mkString(", ") +
      " from " + dbTableName + " m" +
      " inner join " + Person.dbTableName + " p on m." + member.dbColumnName + " = p." + Person.id.dbColumnName +
      " where m." + this.year.dbColumnName + " = " + year)
    List(head) ::: resultList
  }
}

case class MembershipCount(year: Int, validatedCount: Int)

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
