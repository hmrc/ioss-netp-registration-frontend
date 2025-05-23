package pages.website

import pages.QuestionPage
import play.api.libs.json.JsPath

case object WebsitePage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "website"
}
