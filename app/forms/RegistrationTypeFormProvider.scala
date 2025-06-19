package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.RegistrationType

class RegistrationTypeFormProvider @Inject() extends Mappings {

  def apply(): Form[RegistrationType] =
    Form(
      "value" -> enumerable[RegistrationType]("registrationType.error.required")
    )
}
