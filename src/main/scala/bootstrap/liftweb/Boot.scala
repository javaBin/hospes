package bootstrap.liftweb

import net.liftweb.http.{LiftRules}
import net.liftweb.sitemap.{SiteMap, Menu, Loc}
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import net.liftweb.util.{Props}
import net.liftweb.common.{Full}
import net.liftweb.http.{S}
import javaBin.model._
import util.Random


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
      name =>
        val company = Company.create
        company.name.set(name)
        company.save
        Range(0, Random.nextInt(20) + 1).map{"Person" + _}.map{
          name =>
            val person = Person.create
            person.firstName.set(name)
            person.firstName.set("Personson")
            person.save
            val membership = Membership.create
            membership.person.set(person.id)
            membership.companyPaid.set(company.id)
            membership.year.set(2010)
            membership.save
            person
        }.headOption.foreach{
          person =>
            company.contactPerson.set(person.id)
            company.save
        }
    }

    LiftRules.addToPackages("javaBin")

    val entries =
      List(Menu("Home") / "index") :::
              List(Menu(Loc("Static", Link(List("static"), true, "/static/index"), "Static Content"))) :::
              Person.sitemap :::
              Company.menus :::
              Nil
    LiftRules.setSiteMapFunc(() => SiteMap(entries: _*))

    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.loggedInTest = Full(() => Person.loggedIn_?)

    S.addAround(DB.buildLoanWrapper)
  }
}