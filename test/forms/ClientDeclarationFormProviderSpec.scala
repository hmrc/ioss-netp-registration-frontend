package forms

import forms.behaviours.CheckboxFieldBehaviours
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import models.ClientDeclaration
import play.api.data.FormError

class ClientDeclarationFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new ClientDeclarationFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "clientDeclaration.error.required"

    behave like checkboxField[ClientDeclaration](
      form,
      fieldName,
      validValues  = ClientDeclaration.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
