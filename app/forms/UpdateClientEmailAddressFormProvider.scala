package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class UpdateClientEmailAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("updateClientEmailAddress.error.required")
        .verifying(maxLength(100, "updateClientEmailAddress.error.length"))
    )
}
