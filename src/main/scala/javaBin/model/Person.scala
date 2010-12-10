package javaBin.model

import net.liftweb.mapper._
import net.liftweb.common._

object Person extends Person with MetaMegaProtoUser[Person] {
  override def dbTableName = "person"
  override def screenWrap = Full(<lift:surround with="default" at="content">
      <lift:bind/>
  </lift:surround>)
  override def signupFields = List(email, firstName, lastName, address, phoneNumber)
  override def fieldOrder = List(email, firstName, lastName, address, phoneNumber)
  override def skipEmailValidation = true
}

class Person extends MegaProtoUser[Person] {
  def getSingleton = Person
  object phoneNumber extends MappedText(this) {
    override def displayName = "Phone number"
  }
  object address extends MappedText(this) {
    override def displayName = "Address"
  }
}
