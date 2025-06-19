package pages.vatEuDetails

import models.TradingNameAndBusinessAddress
import pages.QuestionPage
import play.api.libs.json.JsPath

case object TradingNameAndBusinessAddressPage extends QuestionPage[TradingNameAndBusinessAddress] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "tradingNameAndBusinessAddress"
}
