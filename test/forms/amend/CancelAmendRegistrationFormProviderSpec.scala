package forms.amend

import forms.amend.CancelAmendRegistrationFormProvider
import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class CancelAmendRegistrationFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "cancelAmendRegistration.error.required"
  val invalidKey = "error.boolean"

  val form = new CancelAmendRegistrationFormProvider()()

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
