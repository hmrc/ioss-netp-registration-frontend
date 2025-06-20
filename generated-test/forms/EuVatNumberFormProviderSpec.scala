package forms

import forms.behaviours.IntFieldBehaviours
import play.api.data.FormError

class EuVatNumberFormProviderSpec extends IntFieldBehaviours {

  val form = new EuVatNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = Int.MaxValue

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like intField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "euVatNumber.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "euVatNumber.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "euVatNumber.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "euVatNumber.error.required")
    )
  }
}
