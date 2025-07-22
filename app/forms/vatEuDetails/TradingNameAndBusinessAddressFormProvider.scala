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
      "tradingName" -> text("tradingNameAndBusinessAddress.error.tradingName.required")
        .verifying(firstError(
          maxLength(40, "tradingNameAndBusinessAddress.error.tradingName.length"),
          regexp(commonTextPattern, "tradingNameAndBusinessAddress.error.tradingName.invalid")
        )),
      "line1" -> text("tradingNameAndBusinessAddress.error.line1.required")
        .verifying(maxLength(35, "tradingNameAndBusinessAddress.error.line1.length"))
        .verifying(regexp(commonTextPattern, "tradingNameAndBusinessAddress.error.line1.format")),
      "line2" -> optional(text("tradingNameAndBusinessAddress.error.line2.required")
        .verifying(maxLength(35, "tradingNameAndBusinessAddress.error.line2.length"))
        .verifying(regexp(commonTextPattern, "tradingNameAndBusinessAddress.error.line2.format"))),
      "townOrCity" -> text("tradingNameAndBusinessAddress.error.townOrCity.required")
        .verifying(maxLength(35, "tradingNameAndBusinessAddress.error.townOrCity.length"))
        .verifying(regexp(commonTextPattern, "tradingNameAndBusinessAddress.error.townOrCity.format")),
      "stateOrRegion" -> optional(text("tradingNameAndBusinessAddress.error.stateOrRegion.required")
        .verifying(maxLength(35, "tradingNameAndBusinessAddress.error.stateOrRegion.length"))
        .verifying(regexp(commonTextPattern, "tradingNameAndBusinessAddress.error.stateOrRegion.format"))),
      "postCode" -> optional(text("tradingNameAndBusinessAddress.error.postCode.required")
        .verifying(firstError(
          maxLength(40, "tradingNameAndBusinessAddress.error.postCode.length"),
          regexp(postcodePattern, "tradingNameAndBusinessAddress.error.postCode.invalid"))))
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

