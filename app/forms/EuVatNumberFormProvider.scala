package forms

import forms.mappings.Mappings
import javax.inject.Inject
import play.api.data.Form

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
