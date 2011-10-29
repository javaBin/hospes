package javaBin
package rest

import util.CreateMembers
import model.{Membership, Person}

import org.specs.Specification
import org.specs.matcher.Matcher

import bootstrap.liftweb.Boot
import net.liftweb.mapper._
import net.liftweb.util.{StringHelpers, Mailer, Props}
import net.liftweb.common.Empty
import javax.mail.internet.MimeMessage
import java.util.concurrent.{TimeUnit, CountDownLatch}
import javax.mail.Message.RecipientType
import net.liftweb.http._

object MembershipResourceSpec extends Specification {
  Props.mode
  Boot.databaseSetup()

  new SpecContext {
    def around(a: => Any) = {
      val testModeSend = Mailer.testModeSend.default.is
      val session = new LiftSession("", StringHelpers.randomString(20), Empty)
      S.initIfUninitted(session)(a)
      Mailer.testModeSend.default.set(testModeSend)
    }
    aroundExpectations(around(_))
  }

  def mail(f: => Unit) = {
    val latch = new CountDownLatch(1)
    var msg:MimeMessage = null
    Mailer.testModeSend.default.set((message:MimeMessage) => {
      latch.countDown()
      msg = message
    })
    f
    latch.await(5, TimeUnit.SECONDS) must_== true
    msg
  }

  def beSentTo(email:String) = new Matcher[MimeMessage]{
    def apply(msg: => MimeMessage) = {
      val message = msg
      val to = message.getRecipients(RecipientType.TO).getValue().apply(0).toString
      (to == email, to + " == " + email, to + " != " + email)
    }
  }


  "posting to membership resource" should {
    setSequential()

    val email = "person@somewhere.com"
    def personWithEmail(email:String) = Person.find(By(Person.email, email))
    def howMany = personWithEmail(email).size
    def json = CreateMembers.createJson(email)

    "result in a new person and new membersships when email address is new" in {
      mail {
        howMany must be equalTo (0)

        MembershipResource.createMembership(json).toResponse.code must be equalTo (200)
        howMany must be equalTo (1)
        Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo (6)
      } must beSentTo(email)
    }

    "result in same person with added memberships for existing address" in {
      mail {
        MembershipResource.createMembership(json).toResponse.code must be equalTo (200)
        howMany must be equalTo (1)
        Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo (12)
      } must beSentTo(email)
    }

    "result in bad request error when membership is missing" in {
      val json = CreateMembers.createJson(itemAmount = 0)
      MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
    }

    "results in bad request error when email-address is missing" in {
      val json = CreateMembers.createJson(omitEmail = true)
      MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
    }

    "results in bad request error when invalid email-address" in {
      val json = CreateMembers.createJson(email = "junkaddress")
      MembershipResource.createMembership(json).toResponse.code must be equalTo (400)
    }
  }

  "looking up memberships" should {
    setSequential()

    def req(path: List[String]) = new Req(
      ParsePath(path, "", false, false), "", GetRequest, Empty, null,
      System.nanoTime, System.nanoTime, false,
      () => ParamCalcInfo(Nil, Map.empty, Nil, Empty), Map())


    def response(email: String) = MembershipResource(req(List("rest", "memberships", email)))().open_!.toResponse.code

    val email = "membershiptest@eventsystems.com"
    var person = Person.create.email(email).firstName("firstname").lastName("lastName").validated(true).password("xxxxxx").saveMe

    "return 404 for unknown person" in {
      response("unknown@man.no") must_== 404
    }

    "return 404 for known person without membership" in {
      response(email) must_== 404
    }

    "return 404 for known person with unverified membership" in {
      val unverifiedEmail = "membershiptest@eventsystems.kom"
      var unverifiedPerson = Person.create.email(unverifiedEmail).firstName("firstname").lastName("lastName").password("xxxxxx").saveMe
      Membership.create.member(unverifiedPerson.id).boughtBy(person.id).save
      response(unverifiedEmail) must_== 404
    }

    "return 204 for known person with verified membership" in {
      Membership.create.member(person.id).boughtBy(person.id).save
      response(email) must_== 204
    }
  }

}