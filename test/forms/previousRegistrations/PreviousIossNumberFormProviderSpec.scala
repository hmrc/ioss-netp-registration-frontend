/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.previousRegistrations

import forms.behaviours.StringFieldBehaviours
import models.Country
import play.api.data.FormError

class PreviousIossNumberFormProviderSpec extends StringFieldBehaviours {

  private val country = Country.euCountries.head
  private val form = new PreviousIossNumberFormProvider()(country)
  private val fieldName = "value"

  ".value" - {

    "must bind valid data" in {
      val validInput = "IM0401234567"
      val result = form.bind(Map(fieldName -> validInput))
      result.errors mustBe empty
      result.value.value mustBe "IM0401234567"
    }

    "must trim whitespace and uppercase input" in {
      val messyInput = "  im040 123 456 7 "
      val result = form.bind(Map(fieldName -> messyInput))
      result.errors mustBe empty
      result.value.value mustBe "IM0401234567"
    }

    "must not bind empty input" in {
      val result = form.bind(Map(fieldName -> ""))
      result.errors must contain only FormError(fieldName, "previousIossNumber.error.schemeNumber.required", Seq(country.name))
    }

    "must return an error for invalid format" in {
      val invalidInput = "INVALID!@#"
      val result = form.bind(Map(fieldName -> invalidInput))
      result.errors must contain only FormError(fieldName, "previousIossNumber.error.invalid")
    }
  }
}