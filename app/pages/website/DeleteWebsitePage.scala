package pages.website

import pages.QuestionPage
import play.api.libs.json.JsPath

case object DeleteWebsitePage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "deleteWebsite"
}
