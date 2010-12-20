package bootstrap.liftweb

import net.liftweb.sitemap.{SiteMap, Menu}
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import javaBin.model._
import net.liftweb.http.{UnauthorizedResponse, LiftRules, S}
import net.liftweb.widgets.autocomplete.AutoComplete
import net.liftweb.common.Full
import net.liftweb.util.{Mailer, Props}

class Boot {
  def boot {
    AutoComplete.init

    Mailer.hostFunc = () => Props.get("smtp.host") openOr "localhost"

    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:mem:db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    Schemifier.schemify(true, Schemifier.infoF _, Person, Company, Membership)
    (0 to 10).map {
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
          (0 to 10).map {
            _ =>
              val membership = Membership.create
              membership.year.set(if (personNumber == 0) 2010 else 2009)
              membership.boughtBy.set(person.id)
              membership.save
          }
        }
        person
    }

    LiftRules.addToPackages("javaBin")

    val unauthorizedResponse = () => new UnauthorizedResponse("No access")
    val entries =
      List(Menu(S.?("home.menu.title")) / "index") :::
      Person.sitemap :::
      List(Menu(S.?("memberships.menu.title")) / "memberships" >> Person.loginFirst >> If(() => Person.currentUser.map(_.thisYearsBoughtMemberships.size > 0).openOr(false), unauthorizedResponse)) :::
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