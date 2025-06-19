package forms

import forms.behaviours.OptionFieldBehaviours
import models.RegistrationType
import play.api.data.FormError

class RegistrationTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new RegistrationTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "registrationType.error.required"

    behave like optionsField[RegistrationType](
      form,
      fieldName,
      validValues  = RegistrationType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
