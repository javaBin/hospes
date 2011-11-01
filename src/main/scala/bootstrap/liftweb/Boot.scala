package bootstrap.liftweb

import java.util.Locale
import java.io.{FileInputStream, File}
import javaBin.OpenIdIntegration
import javaBin.model._
import javax.mail.{PasswordAuthentication, Authenticator}
import net.liftweb.http._
import net.liftweb.http.auth.{userRoles, HttpBasicAuthentication, AuthRole}
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import net.liftweb.sitemap.{SiteMap, Menu, Loc}
import net.liftweb.common.{Logger, Empty, Full}
import net.liftweb.util.{LoggingAutoConfigurer, Mailer, Props}
import javaBin.rest.{MailingListResource, MembershipResource}

class Boot {

  def testModePopulate() {
    (0 to 10).map{
      personNumber =>
        val person = Person.create
        val personName = "Person" + personNumber
        person.email("person" + personNumber + "@lainternet.com")
            .firstName(personName)
            .lastName("Personson")
            .password("passord")
            .superUser(personNumber == 1)
            .validated(personNumber != 2)
            .save()
        if (personNumber == 0 || personNumber == 1) {
          (0 to 10).foreach {
            index =>
              val membership = Membership.create
              membership.year(if (personNumber == 0) 2011 else 2010).boughtBy(person.id)
              if (personNumber == 0 && index == 0)
                membership.member(person.id)
              membership.save()
          }
        }
        person
    }
  }

  def restAuthenticationSetup() {
    val webshopRole = AuthRole("webshop")
    val webshopUser = Props.get("webshop.user").open_!
    val webshopPwd = Props.get("webshop.password").open_!
    val mailingListUser = Props.get("mailing.list.user").open_!
    val mailingListPwd = Props.get("mailing.list.password").open_!
    val mailingListRole = AuthRole("mailinglist")

    LiftRules.authentication = HttpBasicAuthentication("lift") {
      case (`webshopUser`, `webshopPwd`, _) =>
        userRoles(webshopRole :: Nil)
        true
      case (`mailingListUser`, `mailingListPwd`, _) =>
        userRoles(mailingListRole :: Nil)
        true
    }

    LiftRules.httpAuthProtectedResource.append {
      case Req("rest" :: "mailingLists" :: _, _, _) => Full(mailingListRole)
      case Req("rest" :: "memberships" :: _, _, _) => Full(webshopRole)
    }
  }

  def setupEmail() {
    System.setProperty("mail.smtp.host", Props.get("mail.smtp.host").open_!)
    System.setProperty("mail.smtp.port", Props.get("mail.smtp.port").openOr("25"))
    val auth =
      for {
        username <- Props.get("mail.smtp.username")
        password <- Props.get("mail.smtp.password")
      } yield new Authenticator {
        override def getPasswordAuthentication = new PasswordAuthentication(username, password)
      }
    Mailer.authenticator = auth
    System.setProperty("mail.smtp.auth", auth.isDefined.toString)
  }

  def boot() {
    Logger.setup = Full(LoggingAutoConfigurer())

    val localFile = () => {
      val file = new File("/opt/jb/hospes/etc/hospes.props")
      if (file.exists) Full(new FileInputStream(file)) else Empty
    }
    Props.whereToLook = () => (("local", localFile) :: Nil)

    Locale.setDefault(new Locale("nb", "NO"))

    // Needed to get right encoding on subject of mail (and probably other non-xhtml fields)
    System.setProperty("file.encoding", "UTF-8")
    LiftRules.localeCalculator = _ => Locale.getDefault

    LiftRules.liftRequest.append{
      case Req("h2" :: _, _, _) => false
    }

    LiftRules.fixCSS("css" :: "java_membership" :: Nil, Empty)

    setupEmail()
    Boot.databaseSetup()
    Props.mode match {
      case Props.RunModes.Development =>
        testModePopulate()
      case _ =>
    }
    restAuthenticationSetup()

    LiftRules.dispatch.append(MembershipResource)
    LiftRules.dispatch.append(MailingListResource)
    LiftRules.addToPackages("javaBin")

    val entries =
      (Menu(S.?("home.menu.title")) / "index") ::
      ((Menu("hidden") / "openid" / "form") >> Loc.Hidden) ::
      Person.sitemap :::
      (Menu(S.?("admin.menu.title")) / Membership.adminPath >> Person.loginFirst >> Person.isSuperUser) ::
      (Menu(S.?("mailing.lists.menu.title")) / MailingListSubscription.mailingListsPath >> Person.loginFirst) ::
      (Menu(S.?("memberships.menu.title")) / Membership.membershipsPath >> Person.loginFirst >> Person.isMembershipOwner) ::
      Person.logoutMenuLoc.toList

    LiftRules.setSiteMapFunc(() => SiteMap(entries: _*))

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.loggedInTest = Full(() => Person.loggedIn_?)

    // The loginFormUrl must match the openid form entry above
    val openId = new OpenIdIntegration("/openid/form")
    LiftRules.statelessDispatchTable.append(openId.statelessDispatch())
    LiftRules.dispatch.append(openId.dispatch())

    S.addAround(DB.buildLoanWrapper())
  }
}

object Boot {
  def databaseSetup() {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:mem:db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))
      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }
    Schemifier.schemify(true, Schemifier.infoF _, Person, Membership, MailingListSubscription)
  }
}
