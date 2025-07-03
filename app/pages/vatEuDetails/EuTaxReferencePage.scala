package pages.vatEuDetails

import pages.QuestionPage
import play.api.libs.json.JsPath

case object EuTaxReferencePage extends QuestionPage[Int] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "euTaxReference"
}
