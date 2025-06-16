package forms.previousRegistrations

import forms.mappings.Mappings
import models.CheckPreviousSchemeAnswers
import play.api.data.Form

import javax.inject.Inject

class CheckPreviousSchemeAnswersFormProvider @Inject() extends Mappings {

  def apply(): Form[CheckPreviousSchemeAnswers] =
    Form(
      "value" -> enumerable[CheckPreviousSchemeAnswers]("checkPreviousSchemeAnswers.error.required")
    )
}
