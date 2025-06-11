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

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class DeclarationFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "declaration.error.required"
  val invalidKey = "error.boolean"

  val form = new DeclarationFormProvider()()

  ".declaration" - {

    "bind true" in {
      val result = form.bind(Map("declaration" -> "true"))
      result.errors mustBe empty
      result.value.value mustBe true
    }

    "fail to bind false" in {
      val result = form.bind(Map("declaration" -> "false"))
      result.errors must contain only FormError("declaration", requiredKey)
    }

    "fail to bind if not provided" in {
      val result = form.bind(Map.empty[String, String])
      result.errors must contain only FormError("declaration", requiredKey)
    }
  }
}
