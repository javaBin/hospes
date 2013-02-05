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
import net.liftweb.util.{LoggingAutoConfigurer, Mailer, Props}
import javaBin.rest.{MailingListResource, MembershipResource}
import net.liftweb.sitemap.Loc.If
import net.liftweb.common.{Logger, Empty, Full}

class Boot {

  def testModePopulate() {
    val memberships2011P0 = for (i <- 0 until 12) yield Membership.create.year(2011).saveMe()
    val memberships2012P0 = for (i <- 0 until 15) yield Membership.create.year(2012).saveMe()
    val memberships2013P0 = for (i <- 0 until 11) yield Membership.create.year(2013).saveMe()
    val memberships2011P1 = for (i <- 0 until 10) yield Membership.create.year(2011).saveMe()
    val memberships2012P1 = for (i <- 0 until 8) yield Membership.create.year(2012).saveMe()
    val memberships2013P1 = for (i <- 0 until 9) yield Membership.create.year(2013).saveMe()
    (0 until 30).map{
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
        if (personNumber == 0) {
          (memberships2011P0 ++ memberships2012P0 ++ memberships2013P0).map(_.boughtBy(person.id).save())
        } else if (personNumber == 1) {
          (memberships2011P1 ++ memberships2012P1 ++ memberships2013P1).map(_.boughtBy(person.id).save())
        }
        if (personNumber >= 10 && personNumber < 20) {
          memberships2011P0(personNumber - 10).member(person.id).save()
        } else if (personNumber >= 20 && personNumber < 30) {
          memberships2011P1(personNumber - 20).member(person.id).save()
        }
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
//      case Req("rest" :: "mamberships" :: year :: _, _, _) =>
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
//    LiftRules.statelessDispatchTable.append(MembershipResource)
    LiftRules.addToPackages("javaBin")

    val mailingListsDefined = If(
      () => !MailingListEnumeration.values.isEmpty,
      () => new NotFoundResponse("Mailing lists not defined"))
    val entries =
      (Menu(S.?("home.menu.title")) / "index") ::
      ((Menu("hidden") / "openid" / "form") >> Loc.Hidden) ::
      Person.sitemap :::
      (Menu(S.?("admin.menu.title")) / Membership.adminPath >> Person.loginFirst >> Person.isSuperUser) ::
      (Menu(S.?("mailing.lists.menu.title")) / MailingListSubscription.mailingListsPath >> Person.loginFirst >> mailingListsDefined) ::
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
