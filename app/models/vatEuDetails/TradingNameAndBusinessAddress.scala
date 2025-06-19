package models

import play.api.libs.json._

case class TradingNameAndBusinessAddress (field1: String, field2: String)

object TradingNameAndBusinessAddress {

  implicit val format: OFormat[TradingNameAndBusinessAddress] = Json.format
}
