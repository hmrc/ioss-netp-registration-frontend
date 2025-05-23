package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class WebsiteFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("website.error.required")
        .verifying(maxLength(100, "website.error.length"))
    )
}
