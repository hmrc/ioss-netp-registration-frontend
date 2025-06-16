package pages.previousRegistrations

import pages.QuestionPage
import play.api.libs.json.JsPath

case object PreviousOssNumberPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "previousOssNumber"
}
