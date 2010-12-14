package bootstrap.liftweb

import net.liftweb.sitemap.{SiteMap, Menu}
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import net.liftweb.util.{Props}
import net.liftweb.common.{Full}
import javaBin.model._
import util.Random
import net.liftweb.http.{RedirectResponse, UnauthorizedResponse, LiftRules, S}

class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:mem:db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    Schemifier.schemify(true, Schemifier.infoF _, Person, Company, Membership)
    Range(0, 20).map{"Company" + _}.map{
      companyName =>
        val company = Company.create
        company.name.set(companyName)
        if (companyName == "Company0")
          company.hideMembers.set(true)
        company.save
        Range(0, Random.nextInt(20) + 1).map{"Person" + _}.map{
          personName =>
            val person = Person.create
            person.email.set(personName + "@" + companyName + ".com")
            person.firstName.set(personName)
            person.lastName.set("Personson")
            person.password.set("passord")
            person.employer.set(company.id)
            person.validated.set(true)
            person.isContactPerson.set(personName == "Person0")
            person.save
            val membership = Membership.create
            membership.person.set(person.id)
            membership.companyPaid.set(company.id)
            membership.year.set(if (companyName == "Company8") 2009 else 2010)
            membership.save
            person
        }
    }

    LiftRules.addToPackages("javaBin")

    val entries =
      List(Menu("default") / "index" >> Hidden >> EarlyResponse(() => Full(RedirectResponse("user_mgt/login")))) :::
      Person.sitemap :::
      Company.menus :::
      List(Menu("Member list") / "members") :::
      List(Menu("Company") / "companyEdit" >> Person.loginFirst >> If(() => Person.currentUser.map(_.isContactPerson.get).openOr(false), () => new UnauthorizedResponse("No access"))) ::: Nil
    LiftRules.setSiteMapFunc(() => SiteMap(entries: _*))

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.loggedInTest = Full(() => Person.loggedIn_?)

    S.addAround(DB.buildLoanWrapper)
  }
}