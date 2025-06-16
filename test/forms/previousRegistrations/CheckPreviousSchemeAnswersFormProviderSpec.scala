package forms.previousRegistrations

import forms.behaviours.OptionFieldBehaviours
import forms.previousRegistrations.CheckPreviousSchemeAnswersFormProvider
import models.CheckPreviousSchemeAnswers
import play.api.data.FormError

class CheckPreviousSchemeAnswersFormProviderSpec extends OptionFieldBehaviours {

  val form = new CheckPreviousSchemeAnswersFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "checkPreviousSchemeAnswers.error.required"

    behave like optionsField[CheckPreviousSchemeAnswers](
      form,
      fieldName,
      validValues  = CheckPreviousSchemeAnswers.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
