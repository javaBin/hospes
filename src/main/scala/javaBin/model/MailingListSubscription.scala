package javaBin.model

import net.liftweb.mapper._

object MailingListSubscription extends MailingListSubscription with LongKeyedMetaMapper[MailingListSubscription] with CRUDify[Long, MailingListSubscription] {
  val mailingListsPath = "mailing_lists"
}

class MailingListSubscription extends LongKeyedMapper[MailingListSubscription] with IdPK {
  def getSingleton = MailingListSubscription

  object mailingList extends MappedInt(this)

  object member extends LongMappedMapper(this, Person) {
    override def dbColumnName = "member_person_id"
  }

}