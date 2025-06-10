package pages.previousRegistrations

import models.PreviousScheme
import pages.QuestionPage
import play.api.libs.json.JsPath

case object PreviousSchemePage extends QuestionPage[PreviousScheme] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "previousScheme"
}
