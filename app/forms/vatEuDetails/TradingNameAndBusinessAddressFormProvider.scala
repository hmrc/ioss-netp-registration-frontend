/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.vatEuDetails


import forms.mappings.Mappings
import forms.validation.Validation.{commonTextPattern, postcodePattern}
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, InternationalAddress, TradingName}
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class TradingNameAndBusinessAddressFormProvider @Inject() extends Mappings {

  def apply(country: Country): Form[TradingNameAndBusinessAddress] = Form(
    mapping(
      "tradingName" -> text("tradingName.error.required")
        .verifying(firstError(
          maxLength(100, "tradingName.error.length"),
          regexp(commonTextPattern, "tradingName.error.invalid")
        )),
      "line1" -> text("TradingNameAndBusinessAddress.error.line1.required")
        .verifying(maxLength(35, "TradingNameAndBusinessAddress.error.line1.length"))
        .verifying(regexp(commonTextPattern, "TradingNameAndBusinessAddress.error.line1.format")),
      "line2" -> optional(text("TradingNameAndBusinessAddress.error.line2.required")
        .verifying(maxLength(35, "TradingNameAndBusinessAddress.error.line2.length"))
        .verifying(regexp(commonTextPattern, "TradingNameAndBusinessAddress.error.line2.format"))),
      "townOrCity" -> text("TradingNameAndBusinessAddress.error.townOrCity.required")
        .verifying(maxLength(35, "TradingNameAndBusinessAddress.error.townOrCity.length"))
        .verifying(regexp(commonTextPattern, "TradingNameAndBusinessAddress.error.townOrCity.format")),
      "stateOrRegion" -> optional(text("TradingNameAndBusinessAddress.error.stateOrRegion.required")
        .verifying(maxLength(35, "TradingNameAndBusinessAddress.error.stateOrRegion.length"))
        .verifying(regexp(commonTextPattern, "TradingNameAndBusinessAddress.error.stateOrRegion.format"))),
      "postCode" -> optional(text("TradingNameAndBusinessAddress.error.postCode.required")
        .verifying(firstError(
          maxLength(40, "TradingNameAndBusinessAddress.error.postCode.length"),
          regexp(postcodePattern, "TradingNameAndBusinessAddress.error.postCode.invalid"))))
    ) {
      (tradingName, line1, line2, townOrCity, stateOrRegion, postCode) =>
        TradingNameAndBusinessAddress(
          TradingName(tradingName),
          InternationalAddress(line1, line2, townOrCity, stateOrRegion, postCode, Some(country))
        )
    } (model =>
          Some((
            model.tradingName.name,
            model.address.line1,
            model.address.line2,
            model.address.townOrCity,
            model.address.stateOrRegion,
            model.address.postCode
          ))
  ))
}

