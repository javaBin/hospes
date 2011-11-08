package javaBin.rest

import javaBin.model.{Person, MailingListSubscription, MailingListEnumeration}
import net.liftweb.http.OkResponse
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.{Full, Box}

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

    case PlainTextPut("rest" :: "mailingLists" :: mailingListName :: Nil, (text, _)) => {
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
        OkResponse()
      }
    }
  }

}