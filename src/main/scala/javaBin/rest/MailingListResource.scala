package javaBin.rest

import javaBin.model.{Person, MailingListSubscription, MailingListEnumeration}
import net.liftweb.mapper.{By, MappedEmail}
import net.liftweb.common.{Full, Box}
import net.liftweb.http.{Req, NotFoundResponse, NoContentResponse}

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

    case Get("rest" :: "mailingLists" :: mailingListName :: _ :: Nil, req) => {
      val email = getName(req)
      Box(MailingListEnumeration.find(mailingListName)).flatMap {
        _ =>
          Person.find(By(Person.email, email)).map {
            person: Person =>
              if (person.mailingList(mailingListName).checked.is)
                new NoContentResponse()
              else
                new NotFoundResponse("User with email " + email + " not found in mailinglist " + mailingListName)
          }.or(Full(new NotFoundResponse("User with email " + email + " not found")))
      }.or(Full(new NotFoundResponse("Mailing list " + mailingListName + " does not exist")))
    }

    case Put("rest" :: "mailingLists" :: mailingListName :: _ :: Nil, req) => {
      val email = getName(req)
      if (!MappedEmail.emailPattern.matcher(email).matches()) {
        Full(ExplicitBadResponse("Email " + email + " doesn't match the email pattern"))
      } else if (MailingListEnumeration.find(mailingListName) == None) {
        Full(NotFoundResponse("No mailing list " + mailingListName))
      } else for {
        person <- Person.find(By(Person.email, email)).or {
          Full(Person.create.email(email).saveMe())
        }
      } yield {
        person.mailingList(mailingListName).checked(true).save()
        NoContentResponse()
      }
    }
  }

  def getName(req: Req): String = {
    val suffix = req.path.suffix
    (req.path.partPath.last + (if (suffix.length == 0) "" else "." + suffix)).trim.toLowerCase
  }

}