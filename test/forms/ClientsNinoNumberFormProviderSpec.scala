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

package forms

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

class ClientsNinoNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "clientsNinoNumber.error.nino.required"
  val lengthKey = "clientsNinoNumber.error.nino.length"
  val invalidKey = "clientsNinoNumber.error.nino.invalid"
  val specialCharKey = "clientsNinoNumber.error.nino.special.character"
  val validNinoGen: Gen[String] = Gen.oneOf(
    "AA123456A",
    "AB123456C",
    "aa123456b",
    "ab 12 34 56 d"
  )

  val form = new ClientsNinoNumberFormProvider()()
  val fieldName = "value"

  ".value" - {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validNinoGen
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "fail to bind when special characters are present" in {
      val result = form.bind(Map(fieldName -> "AA1234$6A"))
      result.errors must contain(FormError(fieldName, specialCharKey))
    }

    "fail to bind when length is not 9 characters" in {
      val result = form.bind(Map(fieldName -> "AA12345A")) // Only 8 characters
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "fail to bind when NINO format is invalid" in {
      val result = form.bind(Map(fieldName -> "123456789"))
      result.errors must contain(FormError(fieldName, invalidKey))
    }
  }
}
