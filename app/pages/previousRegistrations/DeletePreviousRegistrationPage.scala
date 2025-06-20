package pages.previousRegistrations

import models.DeletePreviousRegistration
import pages.QuestionPage
import play.api.libs.json.JsPath

case object DeletePreviousRegistrationPage extends QuestionPage[DeletePreviousRegistration] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "deletePreviousRegistration"
}
