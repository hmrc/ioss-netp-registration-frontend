package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import forms.previousRegistrations.PreviousEuCountryFormProvider
import models.PreviousEuCountry
import play.api.data.FormError

class PreviousEuCountryFormProviderSpec extends OptionFieldBehaviours {

  val form = new PreviousEuCountryFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "previousEuCountry.error.required"

    behave like optionsField[PreviousEuCountry](
      form,
      fieldName,
      validValues  = PreviousEuCountry.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
