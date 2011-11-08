package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.sitemap.{Loc, Menu}
import net.liftweb.sitemap.Loc._
import net.liftweb.util.{Props, Mailer}
import net.liftweb.util.Mailer._
import net.liftweb.util.Helpers._
import net.liftweb.http.{TemplateFinder, ForbiddenResponse, S}
import scala.xml.NodeSeq
import scala.util.Random
import scala.math._

object Person extends Person with MetaMegaProtoUser[Person] {
  private val forbiddenResponse = () => new ForbiddenResponse("No access")
  val isSuperUser = If(() => Person.currentUser.map(_.superUser.is).openOr(false), forbiddenResponse)
  val isMembershipOwner = If(() => Person.currentUser.map(user => user.thisYearsBoughtMemberships.size > 0).openOr(false), forbiddenResponse)

  override def dbTableName = "person"
  override def screenWrap = Full(
    <lift:surround with="default" at="content">
        <lift:bind/>
    </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber, password)
  override def editFields = signupFields
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)

  override def signupMailSubject = S.?("sign.up.confirmation")
  override def passwordResetEmailSubject = S.?("reset.password.request")

  override def beforeCreate = doBeforeCreate _ :: super.beforeCreate

  private def doBeforeCreate(person: Person) {
    person.openIdKey(abs(Random.nextLong()))
  }

  lazy val newMemberConfirmationPath = thePath("new_member_confirmation")
  def newMemberConfirmation(id: String) = {
    find(By(uniqueId, id)) match {
      case Full(user: Person) =>
        user.validated(true).save()
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
  override protected def lostPasswordMenuLocParams = Hidden :: super.lostPasswordMenuLocParams
  override protected def logoutMenuLocParams = Hidden :: super.logoutMenuLocParams
  override protected def loginMenuLocParams = Hidden :: super.loginMenuLocParams
  override lazy val sitemap = List(loginMenuLoc, createUserMenuLoc, lostPasswordMenuLoc, newMemberConfirmationMenuLoc, editUserMenuLoc, changePasswordMenuLoc, validateUserMenuLoc, resetPasswordMenuLoc).flatten(a => a)

  override def emailFrom = Props.get("mail.from", super.emailFrom)

  override def loginXhtml = super.loginXhtml % ("class" -> "lift-form")
  override def editXhtml(user: Person) = super.editXhtml(user) % ("class" -> "lift-form")
  override def changePasswordXhtml = super.changePasswordXhtml % ("class" -> "lift-form")
  override def lostPasswordXhtml = super.lostPasswordXhtml % ("class" -> "lift-form")
  override def passwordResetXhtml = super.passwordResetXhtml % ("class" -> "lift-form")

  override def signupXhtml(user: Person) =
    <div>
      <h2>
        {S.??("sign.up")}
      </h2>
      <form method="post" action={S.uri}>
        <table>
          {localForm(user, false, signupFields)}<tr>
          <td>
            &nbsp;
          </td>
          <td>
              <user:submit/>
          </td>
        </tr>
        </table>
      </form>
    </div>

  def javaBinStandardGreeting: NodeSeq = (
          <p>
            Med vennlig hilsen
              <br/>
            javaBin
          </p>
                  <p>
                    Kontaktinfo:
                      <br/>
                    portal@java.no
                      <br/>
                    www.java.no
                  </p>)
}

class Person extends MegaProtoUser[Person] with OneToMany[Long, Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = S.?("phone.number")
  }
  object address extends MappedText(this) {
    override def displayName = S.?("address")
  }
  object openIdKey extends MappedLong(this) {
  }

  def name = Seq(firstName, lastName).mkString(" ")
  def nameBox = if (firstName.get.isEmpty && lastName.get.isEmpty) Empty else Full(name)
  def mostPresentableName = nameBox.openOr(email.get)

  def mailingList(mailingListName: String) = {
    MailingListSubscription.findAll(
      By(MailingListSubscription.member, this.id),
      By(MailingListSubscription.mailingList, mailingListName)
    ).headOption.getOrElse {
      MailingListSubscription.create.member(this).mailingList(mailingListName)
    }
  }

  def mailingLists = {
    val existing = MailingListSubscription.findAll(By(MailingListSubscription.member, this.id))
    MailingListEnumeration.values.toList.map {
      value =>
        existing.find(_.mailingList.is == value.toString).getOrElse {
          MailingListSubscription.create.member(this).mailingList(value.toString)
        }
    }
  }

  def thisYearsBoughtMemberships =
    Membership.findAll(
      By(Membership.boughtBy, this.id),
      By(Membership.year, Membership.currentYear),
      OrderBy(Membership.id, Ascending))
  def hasActiveMembership = Membership.find(By(Membership.member, this.id), By(Membership.year, Membership.currentYear)) != Empty

  def template(name: String): NodeSeq = TemplateFinder.findAnyTemplate(List("templates-hidden", name)).open_!

  def sendMembershipRenewedConfirmationEmail(other: Person) {
    val editPath = S.hostAndPath + Person.editPath.mkString("/", "/", "")
    mailMe(bind("info", template("mail-membership-assigned-old-user"),
      "member" -> shortName,
      "boughtBy" -> other.shortName,
      "footer" -> Person.javaBinStandardGreeting,
      "userEditLink" -> <a target="_blank" href={editPath}>
        {editPath}
      </a>))
  }

  def mailMe(xhtml: NodeSeq) {
    Mailer.sendMail(
      From(Person.emailFrom),
      Subject((xhtml \\ "title").text.trim),
      (To(email) :: xmlToMailBodyType(xhtml) :: (Person.bccEmail.toList.map(BCC(_)))): _*)
  }

  def confirmationLink = S.hostAndPath + Person.newMemberConfirmationPath.mkString("/", "/", "/") + uniqueId

  def sendNewMemberConfirmationEmail(other: Person) {
    mailMe(bind("info", template("mail-membership-assigned-new-user"),
      "boughtBy" -> other.shortName,
      "footer" -> Person.javaBinStandardGreeting,
      "userVerification" -> confirmationLink))
  }

  def sendMembershipsReceivedEmail() {
    val membershipLink = (S.hostAndPath :: Membership.membershipsPath :: Nil).mkString("/")
    mailMe(bind("info", template("mail-memberships-received-old-user"),
      "boughtBy" -> shortName,
      "footer" -> Person.javaBinStandardGreeting,
      "memberships" -> <a target="_blank" href={membershipLink}>
        {membershipLink}
      </a>));
  }

  def sendMembershipsReceivedAndUserCreateEmail() {
    mailMe(bind("info", template("mail-memberships-received-new-user"),
      "boughtBy" -> shortName,
      "footer" -> Person.javaBinStandardGreeting,
      "userVerification" -> <a target="_blank" href={confirmationLink}>
        {confirmationLink}
      </a>))
  }
}
