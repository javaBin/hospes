package bootstrap.liftweb

import net.liftweb.sitemap.{SiteMap, Menu}
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import javaBin.model._
import net.liftweb.common.Full
import net.liftweb.util.{Mailer, Props}
import javaBin.rest.MembershipResource
import javax.mail.{PasswordAuthentication, Authenticator}
import net.liftweb.http._
import auth.{userRoles, HttpBasicAuthentication, AuthRole}

class Boot {

  def testModePopulate: Unit = {
    (0 to 10).map{
      personNumber =>
        val person = Person.create
        val personName = "Person" + personNumber
        person.email.set(personName + "@lainternet.com")
        person.validated.set(true)
        person.firstName.set(personName)
        person.lastName.set("Personson")
        person.password.set("passord")
        person.save
        if (personNumber == 0 || personNumber == 1) {
          (0 to 10).map{
            _ =>
              val membership = Membership.create
              membership.year.set(if (personNumber == 0) 2011 else 2010)
              membership.boughtBy.set(person.id)
              membership.save
          }
        }
        person
    }
  }

  def mailSetup: Unit = {
    System.setProperty("mail.smtp.host", "smtp.domeneshop.no")
    System.setProperty("mail.smtp.auth", "true")
    System.setProperty("mail.smtp.port", "587")
    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication("eventsystems5", "VvM8TKJB")
    })
  }

  def restAuthenticationSetup: Unit = {
    val systemRole = AuthRole("system")
    val webshopUser = Props.get("webshop.user").openOr("webshop")
    val webshopPwd = Props.get("webshop.password").openOr("webschopp")
    LiftRules.authentication = HttpBasicAuthentication("lift") {
      case (user, pwd, req) =>
        if (user == webshopUser && pwd == webshopPwd) {
          userRoles(systemRole :: Nil)
          true
        } else
          false
    }
    LiftRules.httpAuthProtectedResource.append{
      case Req("rest" :: _, _, _) => Full(systemRole)
    }
  }

  def boot {
    mailSetup
    Boot.databaseSetup
    testModePopulate
    restAuthenticationSetup

    LiftRules.dispatch.append(MembershipResource)
    LiftRules.addToPackages("javaBin")

    val unauthorizedResponse = () => new UnauthorizedResponse("No access")
    val entries =
      List(Menu(S.?("home.menu.title")) / "index") :::
      Person.sitemap :::
      List(Menu(S.?("memberships.menu.title")) / Membership.membershipsPath >> Person.loginFirst >> If(() => Person.currentUser.map(_.thisYearsBoughtMemberships.size > 0).openOr(false), unauthorizedResponse)) :::
      List(Person.logoutMenuLoc).flatten(a => a) :::
      Nil
    LiftRules.setSiteMapFunc(() => SiteMap(entries: _*))

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.loggedInTest = Full(() => Person.loggedIn_?)

    S.addAround(DB.buildLoanWrapper)
  }
}

object Boot {
  def databaseSetup: Unit = {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:mem:db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))
      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }
    Schemifier.schemify(true, Schemifier.infoF _, Person, Company, Membership)
  }
}
