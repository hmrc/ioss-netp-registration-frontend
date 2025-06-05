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

import forms.behaviours.StringFieldBehaviours
import forms.validation.Validation.commonTextPattern
import models.Country
import play.api.data.FormError

class ClientBusinessNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "clientBusinessName.error.required"
  val lengthKey = "clientBusinessName.error.length"
  val invalidKey = "clientBusinessName.error.invalid"
  val maxLength = 40
  val validData = "Scrumptious Cake Company"
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  val countryName: String = country.name
  val requiredError: FormError = FormError("value", requiredKey, Seq(countryName))

  val form = new ClientBusinessNameFormProvider()(Some(country))

  ".value" - {

    val fieldName = "value"

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

    behave like mandatoryField(
      form,
      "value",
      requiredError = requiredError
    )

    "must not bind invalid Trading Name" in {
      val invalidTradingName = "^Invalid~ tr@ding=nam£"
      val result = form.bind(Map(fieldName -> invalidTradingName)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }
  }
}
