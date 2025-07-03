package forms.vatEuDetails

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

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
