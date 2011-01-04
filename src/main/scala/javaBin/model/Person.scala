package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http.S
import net.liftweb.util.Mailer
import Mailer._
import net.liftweb.sitemap.{Loc, Menu}
import Loc._
import net.liftweb.sitemap.Loc.{If, Template, LocParam, Hidden}
import xml.Elem

object Person extends Person with MetaMegaProtoUser[Person] {
  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber, password)
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)

  override def signupMailSubject = S.?("sign.up.confirmation")
  override def passwordResetEmailSubject = S.?("reset.password.request")

  lazy val newMemberConfirmationPath = thePath("new_member_confirmation")
  def newMemberConfirmation(id: String) = {
    find(By(uniqueId, id)) match {
      case Full(user) =>
        user.validated.set(true)
        user.save
      case _ =>
    }
    passwordReset(id)
  }
  def newMemberConfirmationMenuLoc: Box[Menu] =
    Full(Menu(Loc("NewMemberConfirmation", (newMemberConfirmationPath, true), S.?("new.member.confirmation"), newMemberConfirmationMenuLocParams)))
  protected def newMemberConfirmationMenuLocParams: List[LocParam[Unit]] =
    Hidden ::
    Template(() => wrapIt(newMemberConfirmation(snarfLastItem))) ::
    If(notLoggedIn_? _, S.??("logout.first")) ::
    Nil
  override def lostPasswordMenuLocParams = Hidden :: super.lostPasswordMenuLocParams
  override lazy val sitemap = List(loginMenuLoc, createUserMenuLoc, lostPasswordMenuLoc, newMemberConfirmationMenuLoc, editUserMenuLoc, changePasswordMenuLoc, validateUserMenuLoc, resetPasswordMenuLoc).flatten(a => a)
}

class Person extends MegaProtoUser[Person] with OneToMany[Long, Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = S.?("phone.number")
  }
  object address extends MappedText(this) {
    override def displayName = S.?("address")
  }
  object memberships extends MappedOneToMany(Membership, Membership.member)
  object boughtMemberships extends MappedOneToMany(Membership, Membership.boughtBy)
  @deprecated("Company is out?")
  object employer extends LongMappedMapper(this, Company) {
    override def dbColumnName = "employer_company_id"
  }
  @deprecated("Company is out?")
  object isContactPerson extends MappedBoolean(this) {
    override def defaultValue = false
  }
  def name = Seq(firstName, lastName).mkString(" ")
  def nameBox = if (firstName.get.size == 0 && lastName.get.size == 0) Empty else Full(name)
  def mostPresentableName = nameBox.getOrElse(email.get)
  def thisYearsBoughtMemberships = boughtMemberships.filter(_.isCurrent)
  def hasActiveMembership = memberships.exists(_.isCurrent)

  def sendMembershipRenewedConfirmationEmail(other: Person) {
    mailMe(
      <html>
        <head>
          <title>{S.?("membership.renewed")}</title>
        </head>
        <body>
          <p>{S.?("dear")}{mostPresentableName},
            <br/>
              <br/>
            {S.?("membership.renewed.body", other.mostPresentableName)}
              <br/> <a href={S.hostAndPath}>{S.hostAndPath}</a>
              <br/>
              <br/>
            {S.?("thank.you")}
          </p>
        </body>
      </html>)
  }

  def mailMe(msgXml: Elem): Unit = {
    Mailer.sendMail(From(Person.emailFrom), Subject(S.?("new.member.confirmation")),
      (To(email) :: xmlToMailBodyType(msgXml) ::
              (Person.bccEmail.toList.map(BCC(_)))): _*)
  }

  def sendNewMemberConfirmationEmail(other: Person) {
    val confirmationLink = S.hostAndPath + Person.newMemberConfirmationPath.mkString("/", "/", "/") + uniqueId
    val msgXml = newMemberConfirmationEmailBody(confirmationLink, other)
    mailMe(msgXml)
  }

  def newMemberConfirmationEmailBody(confirmationLink: String, other: Person) = {
    (<html>
      <head>
        <title>{S.?("new.member.confirmation")}</title>
      </head>
      <body>
        <p>{S.?("dear")}{mostPresentableName},<br/>
            <br/>
          {S.?("click.new.member.confirmation.link", other.mostPresentableName)}<br/>
            <br/>
          <a href={confirmationLink}>{confirmationLink}</a> <br/>
            <br/>
          {S.?("thank.you")}
        </p>
      </body>
    </html>)
  }

  def sendSubscriptionsReceivedEmail {
    val subscriptionLink = (S.hostAndPath :: Membership.membershipsPath :: Nil).mkString("/")
    mailMe(newSubscriptionsReceivedEmail(subscriptionLink))
  }

  def newSubscriptionsReceivedEmail(subscriptionLink: String) = {
    (<html>
      <head>
        <title>{S.?("new.subscriptions.received")}</title>
      </head>
      <body>
        <p>{S.?("dear")}{mostPresentableName},<br/>
            <br/>
          {S.?("new.subscriptions.received.body")}<br/>
            <br/>
          <a href={subscriptionLink}>{subscriptionLink}</a> <br/>
            <br/>
          {S.?("thank.you")}
        </p>
      </body>
    </html>)
  }

}
