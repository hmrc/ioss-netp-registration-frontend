package forms.previousRegistrations

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class AddPreviousRegistrationFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "addPreviousRegistration.error.required"
  val invalidKey = "error.boolean"

  val form = new AddPreviousRegistrationFormProvider()()

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
