package javaBin.rest

import net.liftweb.http.NotFoundResponse
import net.liftweb.common.Full
import javaBin.model.{MailingListSubscription, MailingListEnumeration}
import net.liftweb.mapper.By

object MailingListResource extends BetterRestHelper {

  serve {
    case CsvGet("rest" :: "mailingLists" :: listName :: Nil, req) =>
      Full(MailingListEnumeration.values.find(listName == _.toString).map {
        mailingList =>
          CsvResponse(
            for {
              subscription <- MailingListSubscription.findAll(By(MailingListSubscription.mailingList, mailingList.id))
              member <- subscription.member.obj
            } yield member.email.is :: Nil
        )
      }.headOption.getOrElse(NotFoundResponse()))
    //case CsvPut("rest" :: "mailingLists" :: listName :: Nil, (json, _)) =>
    //      Full(NotFoundResponse())
  }

}