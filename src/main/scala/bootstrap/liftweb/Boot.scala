package bootstrap.liftweb

import net.liftweb.http.{LiftRules}
import net.liftweb.sitemap.{SiteMap, Menu, Loc}
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{Schemifier, DB, StandardDBVendor, DefaultConnectionIdentifier}
import net.liftweb.util.{Props}
import net.liftweb.common.{Full}
import net.liftweb.http.{S}
import javaBin.model._


class Boot {
  def boot {

    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor =
      new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
        Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
        Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, Person, Company)


    // where to search snippet
    LiftRules.addToPackages("javaBin")

    // build sitemap
    val entries =
            List(Menu("Home") / "index") :::
            List(Menu(Loc("Static", Link(List("static"), true, "/static/index"), "Static Content"))) :::
            // the User management menu items
            Person.sitemap :::
            Company.menus :::
            Nil

    LiftRules.setSiteMapFunc(() => SiteMap(entries: _*))

    // set character encoding
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
            Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
            Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)
    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => Person.loggedIn_?)

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

  }
}