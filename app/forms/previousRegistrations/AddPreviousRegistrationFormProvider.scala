package forms.previousRegistrations

import forms.mappings.Mappings
import models.AddPreviousRegistration
import play.api.data.Form

import javax.inject.Inject

class AddPreviousRegistrationFormProvider @Inject() extends Mappings {

  def apply(): Form[AddPreviousRegistration] =
    Form(
      "value" -> enumerable[AddPreviousRegistration]("addPreviousRegistration.error.required")
    )
}
