package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import models.AddPreviousRegistration
import play.api.data.FormError

class AddPreviousRegistrationFormProviderSpec extends OptionFieldBehaviours {

  val form = new AddPreviousRegistrationFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "addPreviousRegistration.error.required"

    behave like optionsField[AddPreviousRegistration](
      form,
      fieldName,
      validValues  = AddPreviousRegistration.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
