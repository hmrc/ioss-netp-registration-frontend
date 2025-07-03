package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

class EuTaxReferenceFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "euTaxReference.error.required",
        "euTaxReference.error.wholeNumber",
        "euTaxReference.error.nonNumeric")
          .verifying(inRange(0, Int.MaxValue, "euTaxReference.error.outOfRange"))
    )
}
