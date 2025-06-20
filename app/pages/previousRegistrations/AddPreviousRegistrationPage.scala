package pages.previousRegistrations

import models.AddPreviousRegistration
import pages.QuestionPage
import play.api.libs.json.JsPath

case object AddPreviousRegistrationPage extends QuestionPage[AddPreviousRegistration] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "addPreviousRegistration"
}
