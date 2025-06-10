package forms.previousRegistrations

import forms.mappings.Mappings
import models.PreviousEuCountry
import play.api.data.Form

import javax.inject.Inject

class PreviousEuCountryFormProvider @Inject() extends Mappings {

  def apply(): Form[PreviousEuCountry] =
    Form(
      "value" -> enumerable[PreviousEuCountry]("previousEuCountry.error.required")
    )
}
