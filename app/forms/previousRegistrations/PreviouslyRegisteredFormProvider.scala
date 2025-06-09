package forms.previousRegistrations

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class PreviouslyRegisteredFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("previouslyRegistered.error.required")
    )
}
