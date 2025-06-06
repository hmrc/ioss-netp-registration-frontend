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
import org.scalacheck.Gen
import play.api.data.FormError

class ClientUtrNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "clientUtrNumber.error.required"
  val lengthKey = "clientUtrNumber.error.length"
  val maxLength = 13
  private val minLength = 10

  val form = new ClientUtrNumberFormProvider()()

  def validUtr: Gen[String] =
    Gen.oneOf(
      Gen.listOfN(10, Gen.numChar).map(_.mkString),
      Gen.listOfN(13, Gen.numChar).map(_.mkString),
      Gen.listOfN(10, Gen.numChar).map("k" + _.mkString),
      Gen.listOfN(10, Gen.numChar).map(_.mkString + "k"),
      Gen.listOfN(13, Gen.numChar).map("k" + _.mkString),
      Gen.listOfN(13, Gen.numChar).map(_.mkString + "k"),
    )

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validUtr
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "bind correct values" in {
      forAll(alphaNumStringWithLength(minLength, maxLength - 1)) {
        (validInput: String) =>
          val result = form.bind(Map(fieldName -> validInput)).apply(fieldName + " ")
          result.errors mustBe empty
      }
    }
  }
}
