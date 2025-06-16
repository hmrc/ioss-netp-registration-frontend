package forms.previousRegistrations

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class PreviousIossNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("previousIossNumber.error.required")
        .verifying(maxLength(100, "previousIossNumber.error.length"))
    )
}
