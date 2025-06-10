package forms.previousRegistrations

import forms.mappings.Mappings
import models.PreviousScheme
import play.api.data.Form

import javax.inject.Inject

class PreviousSchemeFormProvider @Inject() extends Mappings {

  def apply(): Form[PreviousScheme] =
    Form(
      "value" -> enumerable[PreviousScheme]("previousScheme.error.required")
    )
}
