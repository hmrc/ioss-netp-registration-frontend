package pages

import models.RegistrationType
import play.api.libs.json.JsPath

case object RegistrationTypePage extends QuestionPage[RegistrationType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registrationType"
}
