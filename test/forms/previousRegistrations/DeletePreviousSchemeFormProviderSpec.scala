package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import forms.previousRegistrations.DeletePreviousSchemeFormProvider
import models.DeletePreviousScheme
import play.api.data.FormError

class DeletePreviousSchemeFormProviderSpec extends OptionFieldBehaviours {

  val form = new DeletePreviousSchemeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "deletePreviousScheme.error.required"

    behave like optionsField[DeletePreviousScheme](
      form,
      fieldName,
      validValues  = DeletePreviousScheme.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
