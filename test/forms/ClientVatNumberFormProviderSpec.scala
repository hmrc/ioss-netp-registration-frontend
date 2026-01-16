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

class ClientVatNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "clientVatNumber.error.required"
  val lengthKey = "clientVatNumber.error.length"
  val invalidKey = "clientVatNumber.error.invalid"
  val maxLength = 9

  val form = new ClientVatNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.listOfN(maxLength, Gen.numChar).map(_.mkString)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "bind valid VAT numbers" in {
      val validVatNumbers = Seq(
        "123456789",
        " GB123456789",
        "gb 123 456 789",
        "123 456 789",
        " GB 123 456 789 "
      )

      validVatNumbers.foreach { vat =>
        val result = form.bind(Map(fieldName -> vat))
        result.errors mustBe empty
        result.get mustBe "123456789"
      }
    }

    "fail to bind invalid VAT numbers" in {
      val invalidVatNumbers = Seq(
        "12345678", // too short
        "1234567890", // too long
        "ABCDEFGHI", // non-numeric
        "1234A6789", // mixed characters
        "GB1234A6789", // mixed with prefix
        "12 34 56", // too short with spaces
      )

      invalidVatNumbers.foreach { vat =>
        val result = form.bind(Map(fieldName -> vat))
        val errors = result.errors.map(_.message)

        withClue(s"For input [$vat], errors: $errors") {
          errors must contain oneOf("clientVatNumber.error.length", "clientVatNumber.error.invalid")
        }
      }
    }
  }
}

