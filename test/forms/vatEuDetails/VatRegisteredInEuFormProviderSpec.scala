package forms

import forms.behaviours.BooleanFieldBehaviours
import forms.vatEuDetails.VatRegisteredInEuFormProvider
import play.api.data.{Form, FormError}

class VatRegisteredInEuFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey: String = "vatRegisteredInEu.error.required"
  private val invalidKey: String = "error.boolean"

  private val form: Form[Boolean] = new VatRegisteredInEuFormProvider()()

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
