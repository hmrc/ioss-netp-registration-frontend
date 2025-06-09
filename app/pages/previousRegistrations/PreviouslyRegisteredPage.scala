package pages.previousRegistrations

import pages.QuestionPage
import play.api.libs.json.JsPath

case object PreviouslyRegisteredPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "previouslyRegistered"
}
