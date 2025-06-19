package forms.vatEuDetails

import forms.mappings.Mappings
import models.RegistrationType
import play.api.data.Form

import javax.inject.Inject

class RegistrationTypeFormProvider @Inject() extends Mappings {

  def apply(): Form[RegistrationType] =
    Form(
      "value" -> enumerable[RegistrationType]("registrationType.error.required")
    )
}
