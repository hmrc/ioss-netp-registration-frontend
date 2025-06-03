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

package forms

import javax.inject.Inject
import forms.mappings.Mappings
import forms.validation.Validation.{commonTextPattern, postcodePattern}
import models.{Country, InternationalAddress}
import play.api.data.Form
import play.api.data.Forms.*

class ClientBusinessAddressFormProvider @Inject() extends Mappings {

  def apply(country: Option[Country]): Form[InternationalAddress] = Form(
    mapping(
      "line1" -> text("clientBusinessAddress.error.line1.required")
        .verifying(maxLength(35, "clientBusinessAddress.error.line1.length"))
        .verifying(regexp(commonTextPattern, "clientBusinessAddress.error.line1.format")),

      "line2" -> optional(text("clientBusinessAddress.error.line2.required")
        .verifying(maxLength(35, "clientBusinessAddress.error.line2.length"))
        .verifying(regexp(commonTextPattern, "clientBusinessAddress.error.line2.format"))),

      "townOrCity" -> text("clientBusinessAddress.error.townOrCity.required")
        .verifying(maxLength(35, "clientBusinessAddress.error.townOrCity.length"))
        .verifying(regexp(commonTextPattern, "clientBusinessAddress.error.townOrCity.format")),

      "stateOrRegion" -> optional(text("clientBusinessAddress.error.stateOrRegion.required")
        .verifying(maxLength(35, "clientBusinessAddress.error.stateOrRegion.length"))
        .verifying(regexp(commonTextPattern, "clientBusinessAddress.error.stateOrRegion.format"))),

      "postCode" -> optional(text("clientBusinessAddress.error.postCode.required")
        .verifying(firstError(
          maxLength(40, "clientBusinessAddress.error.postCode.length"),
          regexp(postcodePattern, "clientBusinessAddress.error.postCode.invalid")))
        )
    )(InternationalAddress(_, _, _, _, _, country))(a => Some((a.line1, a.line2, a.townOrCity, a.stateOrRegion, a.postCode)))
  )
}
