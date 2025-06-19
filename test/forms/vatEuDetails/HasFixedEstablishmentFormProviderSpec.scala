package forms

import forms.behaviours.BooleanFieldBehaviours
import forms.vatEuDetails.HasFixedEstablishmentFormProvider
import models.Country
import play.api.data.{Form, FormError}

class HasFixedEstablishmentFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey: String = "hasFixedEstablishment.error.required"
  private val invalidKey: String = "error.boolean"

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val form: Form[Boolean] = new HasFixedEstablishmentFormProvider()(country)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, args = Seq(country.name))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, args = Seq(country.name))
    )
  }
}