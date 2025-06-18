package forms.previousRegistrations

import forms.mappings.Mappings
import models.DeletePreviousScheme
import play.api.data.Form

import javax.inject.Inject

class DeletePreviousSchemeFormProvider @Inject() extends Mappings {

  def apply(): Form[DeletePreviousScheme] =
    Form(
      "value" -> enumerable[DeletePreviousScheme]("deletePreviousScheme.error.required")
    )
}
