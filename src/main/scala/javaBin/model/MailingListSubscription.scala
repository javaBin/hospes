package javaBin.model

import net.liftweb.mapper._

object MailingListSubscription extends MailingListSubscription with LongKeyedMetaMapper[MailingListSubscription] with CRUDify[Long, MailingListSubscription] {
  val mailingListsPath = "mailing_lists"
}

class MailingListSubscription extends LongKeyedMapper[MailingListSubscription] with IdPK {
  def getSingleton = MailingListSubscription

  def name = MailingListEnumeration(mailingList.is).toString

  object checked extends MappedBoolean(this) {
    override def dbColumnName = "checked"
  }

  object mailingList extends MappedInt(this) {
    override def dbColumnName = "mailing_list_id"
  }

  object member extends LongMappedMapper(this, Person) {
    override def dbColumnName = "member_person_id"
  }

}