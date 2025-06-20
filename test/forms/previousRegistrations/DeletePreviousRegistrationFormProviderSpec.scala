package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.DeletePreviousRegistration
import play.api.data.FormError

class DeletePreviousRegistrationFormProviderSpec extends OptionFieldBehaviours {

  val form = new DeletePreviousRegistrationFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "deletePreviousRegistration.error.required"

    behave like optionsField[DeletePreviousRegistration](
      form,
      fieldName,
      validValues  = DeletePreviousRegistration.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
