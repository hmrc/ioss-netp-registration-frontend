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

import forms.Validation.{commonTextPattern, emailPattern, telephonePattern}
import forms.behaviours.StringFieldBehaviours
import models.BusinessContactDetails
import play.api.data.{Form, FormError}

class BusinessContactDetailsFormProviderSpec extends StringFieldBehaviours {

  private val form: Form[BusinessContactDetails] = new BusinessContactDetailsFormProvider()()

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

  ".fullName" - {

    val fieldName: String = "fullName"
    val requiredKey: String = "businessContactDetails.error.fullName.required"
    val invalidKey: String = "businessContactDetails.error.fullName.invalid"
    val lengthKey: String = "businessContactDetails.error.fullName.length"
    val maxLength: Int = 100
    val validData: String = businessContactDetails.fullName

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
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind invalid full name" in {

      val invalidFullName = "*@test [name]"
      val result = form.bind(Map(fieldName -> invalidFullName)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }

    "must not allow single double quotes, a curly apostrophe or a regular apostrophe at start of string but allow within the string" in {

      val newFullName = "’Test O'Tester’"
      val result = form.bind(Map(fieldName -> newFullName)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }
  }

  ".telephoneNumber" - {

    val fieldName: String = "telephoneNumber"
    val requiredKey: String = "businessContactDetails.error.telephoneNumber.required"
    val invalidKey: String = "businessContactDetails.error.telephoneNumber.invalid"
    val lengthKey: String = "businessContactDetails.error.telephoneNumber.length"
    val maxLength: Int = 20
    val validData: String = businessContactDetails.telephoneNumber

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
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind invalid telephone data" in {

      val invalidTelephone = "invalid"
      val result = form.bind(Map(fieldName -> invalidTelephone)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(telephonePattern)))
    }
  }

  ".emailAddress" - {

    val fieldName: String = "emailAddress"
    val requiredKey: String = "businessContactDetails.error.emailAddress.required"
    val invalidKey: String = "businessContactDetails.error.emailAddress.invalid"
    val lengthKey: String = "businessContactDetails.error.emailAddress.length"
    val maxLength: Int = 50
    val validData: String = businessContactDetails.emailAddress

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
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind valid email address data with .co.uk" in {

      val validEmail = "test@email.co.uk"
      val result = form.bind(Map(fieldName -> validEmail)).apply(fieldName)
      result.value.value `mustBe` validEmail
      result.errors `mustBe` empty
    }

    "must not bind invalid email address data" in {

      val invalidEmail = "invalid"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with missing @" in {

      val invalidEmail = "email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with @@" in {

      val invalidEmail = "test@@email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with @." in {

      val invalidEmail = "test@.email.com"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }

    "must not bind invalid email address data with missing ." in {

      val invalidEmail = "email@"
      val result = form.bind(Map(fieldName -> invalidEmail)).apply(fieldName)
      result.errors `mustBe` Seq(FormError(fieldName, invalidKey, Seq(emailPattern)))
    }
  }
}
