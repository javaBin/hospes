package javaBin.model

import net.liftweb.mapper.MetaMegaProtoUser
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import xml.{Elem, NodeSeq}

trait PersonWebXhtml extends MetaMegaProtoUser[Person] {
  this: Person =>

  override def loginXhtml = super.loginXhtml % ("class" -> "lift-form")
  override def editXhtml(user: Person) = super.editXhtml(user) % ("class" -> "lift-form")
  override def changePasswordXhtml = super.changePasswordXhtml % ("class" -> "lift-form")
  override def lostPasswordXhtml = super.lostPasswordXhtml % ("class" -> "lift-form")
  override def passwordResetXhtml = super.passwordResetXhtml % ("class" -> "lift-form")

  override def standardSubmitButton(name: String, func: () => Any): Elem = super.standardSubmitButton(name, func) % ("class" -> "btn")

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


}
