package javaBin.rest

import org.specs.Specification
import org.specs.runner.JUnit4
import net.liftweb.mapper._
import bootstrap.liftweb.Boot
import javaBin.CreateMembers
import javaBin.model.{Membership, Person}

class MembershipResourceTest extends JUnit4(MembershipResourceSpec)
object MembershipResourceSpec extends Specification {

  Boot.initiateDatabase
  def personWithEmail(email: String) = Person.find(By(Person.email, email))

  "a post sent with a new email-address result in new person and new memberships" in {
    val email = "person@somewhere.com"
    def howMany = personWithEmail(email).size
    howMany must be equalTo(0)
    val json = CreateMembers.createJson(email)
    MembershipResource.createMembership(json).toResponse.code must be equalTo(200)
    howMany must be equalTo(1)
    Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo(6)

    "a post with old email-address result in same person and extra memberships" in {
      MembershipResource.createMembership(json).toResponse.code must be equalTo(200)
      howMany must be equalTo(1)
      Membership.findAll(By(Membership.boughtBy, personWithEmail(email))).size must be equalTo(12)
    }
  }

  "a post without membership requests results in bad request error" in {
    val json = CreateMembers.createJson(itemAmount = 0)
    MembershipResource.createMembership(json).toResponse.code must be equalTo(400)
  }

  "a post without email-address results in bad request error" in {
    val json = CreateMembers.createJson(omitEmail = true)
    MembershipResource.createMembership(json).toResponse.code must be equalTo(400)
  }

  "a post with invalid email-address results in bad request error" in {
    val json = CreateMembers.createJson(email = "junkaddress")
    MembershipResource.createMembership(json).toResponse.code must be equalTo(400)
  }

}