package forms.previousRegistrations

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class PreviousOssNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("previousOssNumber.error.required")
        .verifying(maxLength(100, "previousOssNumber.error.length"))
    )
}
