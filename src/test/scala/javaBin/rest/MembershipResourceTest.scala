package javaBin.rest

import org.specs.Specification
import org.specs.runner.JUnit4
import net.liftweb.mapper._
import bootstrap.liftweb.Boot
import javaBin.CreateMembers
import javaBin.model.{Membership, Person}
import javax.mail.internet.MimeMessage
import org.specs.matcher.Matcher
import net.liftweb.util.{StringHelpers, Mailer}
import net.liftweb.common.Empty
import net.liftweb.http.{LiftRules, S, LiftSession}

class MembershipResourceTest extends JUnit4(MembershipResourceSpec)

object MembershipResourceSpec extends Specification {

  Boot.initiateDatabase

  def personWithEmail(email: String) = Person.find(By(Person.email, email))

  var lastMessage: Option[MimeMessage] = None
  Mailer.testModeSend.default.set{
    (msg: MimeMessage) =>
      println("Message received: " + msg)
      lastMessage = Some(msg)
  }

  object MatchMessage extends Matcher[Option[MimeMessage]] {
    def apply(msgF: => Option[MimeMessage]) = (msgF != None, "Mail found", "Mail not found")
  }

  val session = new LiftSession("", StringHelpers.randomString(20), Empty)

  def inSession(a: => Any) = {
    println("############ HER #############")
    S.initIfUninitted(session) {
      a
    }
  }

  /*new SpecContext {
    aroundExpectations(inSession(_))
  }*/

  "a post sent with a new email-address result in new person and new memberships" in {
    inSession {
    val email = "person@somewhere.com"
    def howMany = personWithEmail(email).size
    howMany must be equalTo (0)
    val json = CreateMembers.createJson(email)
    MembershipResource.createMembership(json).toResponse.code must be equalTo (200)
    howMany must be equalTo (1)
    //lastMessage must MatchMessage
    Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo (6)

    "a post with old email-address result in same person and extra memberships" in {
      lastMessage = None
      MembershipResource.createMembership(json).toResponse.code must be equalTo (200)
      howMany must be equalTo (1)
      lastMessage must MatchMessage
      Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo (12)
    }           }
  }

  "a post without membership requests results in bad request error" in {
    val json = CreateMembers.createJson(itemAmount = 0)
    MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
  }

  "a post without email-address results in bad request error" in {
    val json = CreateMembers.createJson(omitEmail = true)
    MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
  }

  "a post with invalid email-address results in bad request error" in {
    val json = CreateMembers.createJson(email = "junkaddress")
    MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
  }

}