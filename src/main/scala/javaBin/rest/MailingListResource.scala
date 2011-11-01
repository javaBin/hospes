package javaBin.rest

import javaBin.model.{Person, MailingListSubscription, MailingListEnumeration}
import net.liftweb.http.OkResponse
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.{Full, Box}

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

    case PlainTextPut("rest" :: "mailingLists" :: name :: Nil, (text, _)) => {
      val email = text.trim.toLowerCase
      if (!MappedEmail.emailPattern.matcher(email).matches()) {
        Full(ExplicitBadResponse("Text doesn't match the email pattern"))
      } else for {
        mailingListValue <- Box(findMailingList(name))
        person <- Person.find(By(Person.email, email)).or {
          Full(Person.create.email(email).saveMe())
        }
      } yield {
        person.mailingList(mailingListValue.id).checked(true).save()
        OkResponse()
      }
    }
  }

}