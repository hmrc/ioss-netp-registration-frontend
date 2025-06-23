package forms.vatEuDetails

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class EuVatNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "euVatNumber.error.required",
        "euVatNumber.error.wholeNumber",
        "euVatNumber.error.nonNumeric")
          .verifying(inRange(0, Int.MaxValue, "euVatNumber.error.outOfRange"))
    )
}
