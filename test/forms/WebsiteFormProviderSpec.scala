package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class WebsiteFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "website.error.required"
  val lengthKey = "website.error.length"
  val maxLength = 100

  val form = new WebsiteFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
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
  }
}
