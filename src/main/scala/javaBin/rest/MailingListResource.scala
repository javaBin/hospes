package javaBin.rest

import javaBin.model.{Person, MailingListSubscription, MailingListEnumeration}
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.{Full, Box}
import net.liftweb.http.NoContentResponse

object MailingListResource extends BetterRestHelper {

  serve {
    case CsvGet("rest" :: "mailingLists" :: name :: Nil, req) =>
      Box(MailingListEnumeration.find(name).map {
        mailingListValue =>
          CsvResponse(
            for {
              subscription <- MailingListSubscription.findSubscribers(mailingListValue)
              member <- subscription.member.obj
            } yield member.email.is :: Nil
          )
      })

    case Put("rest" :: "mailingLists" :: mailingListName :: text :: Nil, _) => {
      val email = text.trim.toLowerCase
      if (!MappedEmail.emailPattern.matcher(email).matches()) {
        Full(ExplicitBadResponse("Text doesn't match the email pattern"))
      } else for {
        _ <- Box(MailingListEnumeration.find(mailingListName))
        person <- Person.find(By(Person.email, email)).or {
          Full(Person.create.email(email).saveMe())
        }
      } yield {
        person.mailingList(mailingListName).checked(true).save()
        NoContentResponse()
      }
    }
  }

}