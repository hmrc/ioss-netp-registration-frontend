package pages.previousRegistrations

import models.CheckPreviousSchemeAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath

case object CheckPreviousSchemeAnswersPage extends QuestionPage[CheckPreviousSchemeAnswers] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkPreviousSchemeAnswers"
}
