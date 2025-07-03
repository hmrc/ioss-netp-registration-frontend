package forms

import forms.behaviours.IntFieldBehaviours
import play.api.data.FormError

class EuTaxReferenceFormProviderSpec extends IntFieldBehaviours {

  val form = new EuTaxReferenceFormProvider()()

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
      nonNumericError  = FormError(fieldName, "euTaxReference.error.nonNumeric"),
      wholeNumberError = FormError(fieldName, "euTaxReference.error.wholeNumber")
    )

    behave like intFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "euTaxReference.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "euTaxReference.error.required")
    )
  }
}
