package javaBin.rest

import javaBin.model.{Person, MailingListSubscription, MailingListEnumeration}
import net.liftweb.mapper.By
import net.liftweb.common.Box
import net.liftweb.http.OkResponse

object MailingListResource extends BetterRestHelper {

  def findMailingList(listName: String): Option[MailingListEnumeration.Value] =
    MailingListEnumeration.values.find(listName == _.toString)

  serve {
    case CsvGet("rest" :: "mailingLists" :: name :: Nil, req) =>
      Box(findMailingList(name).map {
        mailingListValue =>
          CsvResponse(
            for {
              subscription <- MailingListSubscription.findSubscribers(mailingListValue)
              member <- subscription.member.obj
            } yield member.email.is :: Nil
          )
      })

    case PlainTextPut("rest" :: "mailingLists" :: name :: Nil, (text, req)) =>
      Box(
        for {
          mailingListValue <- findMailingList(name)
          person <- Person.find(By(Person.email, text.trim.toLowerCase)).toOption
        } yield {
          person.mailingList(mailingListValue.id).checked(true).save()
          OkResponse()
        }
      )
  }

}