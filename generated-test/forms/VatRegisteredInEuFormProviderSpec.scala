package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class VatRegisteredInEuFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "vatRegisteredInEu.error.required"
  val invalidKey = "error.boolean"

  val form = new VatRegisteredInEuFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
