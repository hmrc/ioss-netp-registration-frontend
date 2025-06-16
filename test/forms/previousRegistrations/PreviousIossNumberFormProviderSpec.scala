package forms.previousRegistrations

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class PreviousIossNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "previousIossNumber.error.required"
  val lengthKey = "previousIossNumber.error.length"
  val maxLength = 100

  val form = new PreviousIossNumberFormProvider()()

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
