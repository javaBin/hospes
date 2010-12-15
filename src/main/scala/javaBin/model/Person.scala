package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.openid.{SimpleOpenIDVendor, MetaOpenIDProtoUser, OpenIDProtoUser}
import net.liftweb.http.TemplateFinder

object Person extends Person with MetaOpenIDProtoUser[Person] {
  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber)
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)
  override def skipEmailValidation = true
  override def loginXhtml =
    TemplateFinder.findAnyTemplate("templates-hidden" ::  "loginFormContent" :: Nil).map(
      template =>
        <form method="post">
          {template}
        </form>
    ).openOr(super.loginXhtml)
}

class Person extends OpenIDProtoUser[Person] {
  def getSingleton = Person
  def openIDVendor = SimpleOpenIDVendor
  object phoneNumber extends MappedText(this) {
    override def displayName = "Phone number"
  }
  object address extends MappedText(this) {
    override def displayName = "Address"
  }
  object employer extends LongMappedMapper(this, Company) {
    override def dbColumnName = "employer_company_id"
  }
  object isContactPerson extends MappedBoolean(this) {
    override def defaultValue = false
  }
  def name = Seq(firstName, lastName).mkString(" ")
}
