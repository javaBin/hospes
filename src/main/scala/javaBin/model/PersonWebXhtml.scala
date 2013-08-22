package javaBin.model

import net.liftweb.mapper.MetaMegaProtoUser
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import xml.{Elem, NodeSeq}

trait PersonWebXhtml extends MetaMegaProtoUser[Person] {
  this: Person =>

  override def standardSubmitButton(name: String, func: () => Any): Elem = super.standardSubmitButton(name, func) % ("class" -> "btn")

  override def signupXhtml(user: Person) =
    <div>
      <h2>{S.??("sign.up")}</h2>
      <form method="post" action={S.uri} class="form-horizontal">
        { localForm(user, ignorePassword = false, signupFields) }
        { bootstrapSubmit }
      </form>
    </div>

  override def loginXhtml =
    <div>
      <h2>{S.??("log.in")}</h2>
      <form method="post" action={S.uri} class="form-horizontal">
        { bootstrapField("userName", userNameFieldString, <user:email />) }
        { bootstrapField("password", S.??("password"), <user:password />) }
        <div class="control-group">
          <div class="controls">
            <a href={lostPasswordPath.mkString("/", "/", "")}>{S.??("recover.password")}</a>
          </div>
        </div>
        { bootstrapSubmit }
      </form>
    </div>

  override def lostPasswordXhtml =
    <div>
      <h2>Gjenopprett passord</h2>
      <p>{S.??("enter.email")}</p>
      <form method="post" action={S.uri} class="form-horizontal">
        { bootstrapField("email", userNameFieldString, <user:email />) }
        { bootstrapSubmit }
      </form>
    </div>

  override protected def localForm(user: Person, ignorePassword: Boolean, fields: List[FieldPointerType]): NodeSeq = {
    for {
      pointer <- fields
      field <- computeFieldFromPointer(user, pointer).toList
      if field.show_? && (!ignorePassword || !pointer.isPasswordField_?)
      form <- field.toForm.toList
    } yield bootstrapField(field.name, field.displayName, form)
  }

  override def editXhtml(user: Person) =
    <div>
      <h2>{S.??("edit")}</h2>
      <form method="post" action={S.uri} class="form-horizontal">
        { localForm(user, ignorePassword = true, editFields) }
        { bootstrapSubmit }
      </form>
    </div>

  override def changePasswordXhtml =
    <div>
      <h2>{S.??("change.password")}</h2>
      <form method="post" action={S.uri} class="form-horizontal">
        { bootstrapField("old-password", S.??("old.password"), <user:old_pwd />) }
        { bootstrapField("new-password", S.??("new.password"), <user:new_pwd />) }
        { bootstrapField("repeat-password", S.??("repeat.password"), <user:new_pwd />) }
        { bootstrapSubmit }
      </form>
    </div>

  override def passwordResetXhtml =
    <div>
      <h2>{S.??("reset.your.password")}</h2>
      <form method="post" action={S.uri} class="form-horizontal">
        { bootstrapField("password", S.??("enter.your.new.password"), <user:pwd />) }
        { bootstrapField("repeat-password", S.??("repeat.your.new.password"), <user:pwd />) }
        { bootstrapSubmit }
      </form>
    </div>

  def bootstrapField(fieldId: String, title: String, submitField: NodeSeq) =
    <div class="control-group">
      <label class="control-label" for={ fieldId }>
        { title }
      </label>
      <div class="controls">
        { submitField }
      </div>
    </div>

  def bootstrapSubmit =
    <div class="control-group">
      <div class="controls">
        <user:submit/>
      </div>
    </div>

}
