package forms.vatEuDetails

import forms.mappings.Mappings
import models.TradingNameAndBusinessAddress
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class TradingNameAndBusinessAddressFormProvider @Inject() extends Mappings {

   def apply(): Form[TradingNameAndBusinessAddress] = Form(
     mapping(
      "field1" -> text("tradingNameAndBusinessAddress.error.field1.required")
        .verifying(maxLength(100, "tradingNameAndBusinessAddress.error.field1.length")),
      "field2" -> text("tradingNameAndBusinessAddress.error.field2.required")
        .verifying(maxLength(100, "tradingNameAndBusinessAddress.error.field2.length"))
    )(TradingNameAndBusinessAddress.apply)(x => Some((x.field1, x.field2)))
   )
 }
