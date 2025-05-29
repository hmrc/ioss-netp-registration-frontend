package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class AddWebsiteFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "addWebsite.error.required"
  val invalidKey = "error.boolean"

  val form = new AddWebsiteFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
