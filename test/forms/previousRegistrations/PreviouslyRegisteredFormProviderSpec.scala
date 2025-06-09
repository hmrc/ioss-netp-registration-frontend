package forms.previousRegistrations

import forms.behaviours.BooleanFieldBehaviours
import forms.previousRegistrations.PreviouslyRegisteredFormProvider
import play.api.data.FormError

class PreviouslyRegisteredFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "previouslyRegistered.error.required"
  val invalidKey = "error.boolean"

  val form = new PreviouslyRegisteredFormProvider()()

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
