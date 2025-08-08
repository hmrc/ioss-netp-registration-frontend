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
import forms.clientDeclarationJourney.ClientCodeEntryFormProvider
import play.api.data.FormError

import scala.collection.immutable.ArraySeq

class ClientCodeEntryFormProviderSpec extends StringFieldBehaviours {

  val errorKey = "clientCodeEntry.error"
  val maxLength = 6

  val form = new ClientCodeEntryFormProvider()()

  ".clientCodeEntry" - {


    "Succeed for a 6 digit Alpha code without case sensitivity" in {
      val testCases = Table(
        ("input", "condition"),
        ("abcdef", "lower case"),
        ("ABCDEF", "upper case"),
        ("AbCdEf", "mixed cases"),
      )

      forAll(testCases) { (input: String, condition: String) =>

        val result = form.bind(Map("value" -> input))

        withClue(s"Expected positive result for '$condition':") {
          result.errors mustBe empty
          result.value.value mustBe input
        }
      }
    }

    "Error for any Alpha/Numeric code of any other length" in {
      val testCases = Table(
        ("input", "condition", "failing criteria"),
        ("abcdefg", "Greater than 6 characters", 6),
        ("abcde", "Less than 6 characters", 6),
        ("abcde1", "6 characters but containing numbers", "^[A-Za-z]+$"),
        ("abcde!", "6 characters but containing symbols", "^[A-Za-z]+$")
      )

      forAll(testCases) { (input: String, condition: String, failingCriteria) =>

        val result = form.bind(Map("value" -> input))

        withClue(s"Expected FormError for condition '$condition':") {

          result.errors must contain only FormError("value", List(errorKey), ArraySeq(failingCriteria))

        }
      }
    }
  }
}
