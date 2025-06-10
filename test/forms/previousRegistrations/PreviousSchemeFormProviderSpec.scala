package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import forms.previousRegistrations.PreviousSchemeFormProvider
import models.PreviousScheme
import play.api.data.FormError

class PreviousSchemeFormProviderSpec extends OptionFieldBehaviours {

  val form = new PreviousSchemeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "previousScheme.error.required"

    behave like optionsField[PreviousScheme](
      form,
      fieldName,
      validValues  = PreviousScheme.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
