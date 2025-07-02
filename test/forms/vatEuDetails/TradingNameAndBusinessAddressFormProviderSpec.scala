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

import forms.behaviours.StringFieldBehaviours
import forms.validation.Validation.{commonTextPattern, postcodePattern}
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.Country
import play.api.data.{Form, FormError}

class TradingNameAndBusinessAddressFormProviderSpec extends StringFieldBehaviours {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val form: Form[TradingNameAndBusinessAddress] = new TradingNameAndBusinessAddressFormProvider()(country)

  ".line1" - {

    val fieldName: String = "line1"
    val requiredKey: String = "tradingNameAndBusinessAddress.error.line1.required"
    val lengthKey: String = "tradingNameAndBusinessAddress.error.line1.length"
    val formatKey: String = "tradingNameAndBusinessAddress.error.line1.format"
    val maxLength: Int = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like commonTextField(
      form,
      fieldName,
      FormError(fieldName, formatKey, Seq(commonTextPattern)),
      FormError(fieldName, lengthKey, Seq(maxLength)),
      maxLength
    )
  }

  ".line2" - {

    val fieldName: String = "line2"
    val lengthKey: String = "tradingNameAndBusinessAddress.error.line2.length"
    val formatKey: String = "tradingNameAndBusinessAddress.error.line2.format"
    val maxLength: Int = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like commonTextField(
      form,
      fieldName,
      FormError(fieldName, formatKey, Seq(commonTextPattern)),
      FormError(fieldName, lengthKey, Seq(maxLength)),
      maxLength
    )
  }

  ".townOrCity" - {

    val fieldName: String = "townOrCity"
    val requiredKey: String = "tradingNameAndBusinessAddress.error.townOrCity.required"
    val lengthKey: String = "tradingNameAndBusinessAddress.error.townOrCity.length"
    val formatKey: String = "tradingNameAndBusinessAddress.error.townOrCity.format"
    val maxLength: Int = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like commonTextField(
      form,
      fieldName,
      FormError(fieldName, formatKey, Seq(commonTextPattern)),
      FormError(fieldName, lengthKey, Seq(maxLength)),
      maxLength
    )
  }

  ".stateOrRegion" - {

    val fieldName: String = "stateOrRegion"
    val lengthKey: String = "tradingNameAndBusinessAddress.error.stateOrRegion.length"
    val formatKey: String = "tradingNameAndBusinessAddress.error.stateOrRegion.format"
    val maxLength: Int = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like commonTextField(
      form,
      fieldName,
      FormError(fieldName, formatKey, Seq(commonTextPattern)),
      FormError(fieldName, lengthKey, Seq(maxLength)),
      maxLength
    )
  }

  ".postCode" - {

    val fieldName: String = "postCode"
    val lengthKey: String = "tradingNameAndBusinessAddress.error.postCode.length"
    val invalidKey: String = "tradingNameAndBusinessAddress.error.postCode.invalid"
    val maxLength: Int = 40
    val validData: String = "75023 CEDEX 01"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validData
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    "must not bind invalid postcode" in {

      val invalidPostcode: String = "AB@% *JH"
      val result = form.bind(Map(fieldName -> invalidPostcode)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(postcodePattern)))
    }
  }
}
