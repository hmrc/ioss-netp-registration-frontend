package pages.previousRegistrations

import models.PreviousEuCountry
import pages.QuestionPage
import play.api.libs.json.JsPath

case object PreviousEuCountryPage extends QuestionPage[PreviousEuCountry] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "previousEuCountry"
}
