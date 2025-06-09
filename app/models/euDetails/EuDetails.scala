package models.euDetails

import models.{Country, InternationalAddress}
import play.api.libs.json.{Json, OFormat}

case class EuDetails(
                      euCountry: Country,
                      hasFixedEstablishment: Option[Boolean],
                      registrationType: Option[RegistrationType],
                      euVatNumber: Option[String],
                      euTaxReference: Option[String],
                      fixedEstablishmentTradingName: Option[String],
                      fixedEstablishmentAddress: Option[InternationalAddress],
                    )

object EuDetails {

  implicit val format: OFormat[EuDetails] = Json.format[EuDetails]
}