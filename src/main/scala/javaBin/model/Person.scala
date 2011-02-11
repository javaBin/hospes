package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.sitemap.{Loc, Menu}
import net.liftweb.sitemap.Loc._
import xml.Elem
import net.liftweb.util.{Props, Mailer, Helpers}
import Mailer._
import Helpers._
import net.liftweb.http.{ForbiddenResponse, S}

object Person extends Person with MetaMegaProtoUser[Person] {
  private val forbiddenResponse = () => new ForbiddenResponse("No access")
  val isSuperUser = If(() => Person.currentUser.map(_.superUser.is).openOr(false), forbiddenResponse)
  val isMembershipOwner = If(() => Person.currentUser.map(user => user.thisYearsBoughtMemberships.size > 0).openOr(false), forbiddenResponse)

  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber, password)
  override def editFields = signupFields
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
  override lazy val sitemap = List(createUserMenuLoc, lostPasswordMenuLoc, newMemberConfirmationMenuLoc, editUserMenuLoc, changePasswordMenuLoc, validateUserMenuLoc, resetPasswordMenuLoc).flatten(a => a)

  override def emailFrom = Props.get("mail.from", super.emailFrom)

  override def loginXhtml = super.loginXhtml % ("class" -> "lift-form")
  override def signupXhtml(user: Person) = super.signupXhtml(user) % ("class" -> "lift-form")
  override def editXhtml(user: Person) = super.editXhtml(user) % ("class" -> "lift-form")
  override def changePasswordXhtml = super.changePasswordXhtml % ("class" -> "lift-form")
  override def lostPasswordXhtml = super.lostPasswordXhtml % ("class" -> "lift-form")
}

class Person extends MegaProtoUser[Person] with OneToMany[Long, Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = S.?("phone.number")
  }
  object address extends MappedText(this) {
    override def displayName = S.?("address")
  }
  def name = Seq(firstName, lastName).mkString(" ")
  def nameBox = if (firstName.get.isEmpty && lastName.get.isEmpty) Empty else Full(name)
  def mostPresentableName = nameBox.openOr(email.get)
  def thisYearsBoughtMemberships =
    Membership.findAll(
      By(Membership.boughtBy, this.id),
      By(Membership.year, Membership.currentYear),
      OrderBy(Membership.id, Ascending))
  def hasActiveMembership = Membership.find(By(Membership.member, this.id), By(Membership.year, Membership.currentYear)) != Empty

  def javaBinMailBody(title: String, body: String, link: String) = {
    <html>
      <head>
        <title>{title}</title>
      </head>
      <body>
        <p>{S.?("dear")} {mostPresentableName}, <br/>
            <br/>
          {body}<br/>
            <br/>
          <a href={link}>{link}</a> <br/>
            <br/>
          {S.?("thank.you")}
        </p>
      </body>
    </html>
  }

  def sendMembershipRenewedConfirmationEmail(other: Person) {
    mailMe(javaBinMailBody(
      S.?("membership.renewed"),
      S.?("membership.renewed.body", other.mostPresentableName),
      S.hostAndPath + Person.editPath.mkString("/", "/", "")))
  }

  def mailMe(msgXml: Elem): Unit = {
    Mailer.sendMail(
      From(Person.emailFrom),
      Subject(S.?("new.member.confirmation")),
      (To(email) :: xmlToMailBodyType(msgXml) :: (Person.bccEmail.toList.map(BCC(_)))): _*)
  }

  def confirmationLink = S.hostAndPath + Person.newMemberConfirmationPath.mkString("/", "/", "/") + uniqueId

  def sendNewMemberConfirmationEmail(other: Person) {
    mailMe(javaBinMailBody(
      S.?("new.member.confirmation"),
      S.?("click.new.member.confirmation.link", other.mostPresentableName),
      confirmationLink))
  }

  def sendSubscriptionsReceivedEmail {
    val subscriptionLink = (S.hostAndPath :: Membership.membershipsPath :: Nil).mkString("/")
    mailMe(javaBinMailBody(
      S.?("new.subscriptions.received"),
      S.?("new.subscriptions.received.body"),
      subscriptionLink))
  }

  def sendSubscriptionsReceivedAndUserCreateEmail {
    mailMe(javaBinMailBody(
      S.?("new.subscriptions.received.and.user.created"),
      S.?("new.subscriptions.received.and.user.created.body"),
      confirmationLink))
  }

}
