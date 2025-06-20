package forms.previousRegistrations

import forms.mappings.Mappings
import models.DeletePreviousRegistration
import play.api.data.Form

import javax.inject.Inject

class DeletePreviousRegistrationFormProvider @Inject() extends Mappings {

  def apply(): Form[DeletePreviousRegistration] =
    Form(
      "value" -> enumerable[DeletePreviousRegistration]("deletePreviousRegistration.error.required")
    )
}
