package pages.previousRegistrations

import models.DeletePreviousScheme
import pages.QuestionPage
import play.api.libs.json.JsPath

case object DeletePreviousSchemePage extends QuestionPage[DeletePreviousScheme] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "deletePreviousScheme"
}
