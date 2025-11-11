/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.etmp.amend

import logging.Logging
import models.UserAnswers
import models.etmp.*
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails}
import pages.{ClientHasUtrNumberPage, ClientHasVatNumberPage, ClientTaxReferencePage, ClientsNinoNumberPage}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class EtmpAmendRegistrationRequest(
                                         administration: EtmpAdministration,
                                         changeLog: EtmpAmendRegistrationChangeLog,
                                         customerIdentification: EtmpAmendCustomerIdentification,
                                         tradingNames: Seq[EtmpTradingName],
                                         intermediaryDetails: Option[EtmpIntermediaryDetails],
                                         otherAddress: Option[EtmpOtherAddress],
                                         schemeDetails: EtmpSchemeDetails,
                                         bankDetails: Option[EtmpBankDetails]
                                       )

object EtmpAmendRegistrationRequest extends EtmpCommonRegistrationRequest with Logging {

  implicit val format: OFormat[EtmpAmendRegistrationRequest] = Json.format[EtmpAmendRegistrationRequest]

  def buildEtmpAmendRegistrationRequest(
                                         answers: UserAnswers,
                                         registration: EtmpDisplayRegistration,
                                         commencementDate: LocalDate,
                                         iossNumber: String,
                                         rejoin: Boolean = false
                                       ): EtmpAmendRegistrationRequest = {

    val schemeDetails = getSchemeDetails(answers, commencementDate)
    val taxIdType: EtmpIdType = determineTaxIdType(answers)
    val maybeFtr: Option[String] = answers.get(ClientTaxReferencePage)

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSIntAmendClient),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = registration.tradingNames != getTradingNames(answers),
        fixedEstablishments = compareEuRegistrationDetails(
          registration.schemeDetails.euRegistrationDetails, schemeDetails.euRegistrationDetails,
        ),
        contactDetails = contactDetailsDiff(registration.schemeDetails, getSchemeDetails(answers, commencementDate)),
        bankDetails = false,
        reRegistration = rejoin,
        otherAddress = registration.otherAddress != getOtherAddress(taxIdType, answers)
      ),
      customerIdentification = EtmpAmendCustomerIdentification(iossNumber, maybeFtr),
      tradingNames = getTradingNames(answers),
      intermediaryDetails = None,
      otherAddress = getOtherAddress(taxIdType, answers),
      schemeDetails = getSchemeDetails(answers, commencementDate),
      bankDetails = None
    )
  }

  private def contactDetailsDiff(registrationSchemeDetails: EtmpDisplaySchemeDetails, amendSchemeDetails: EtmpSchemeDetails): Boolean = {
    registrationSchemeDetails.contactName != amendSchemeDetails.contactName ||
      registrationSchemeDetails.businessTelephoneNumber != amendSchemeDetails.businessTelephoneNumber ||
      registrationSchemeDetails.businessEmailId != amendSchemeDetails.businessEmailId
  }

  private def determineTaxIdType(answers: UserAnswers): EtmpIdType = {
    answers.get(ClientHasVatNumberPage) match {
      case Some(true) =>
        EtmpIdType.VRN

      case _ =>
        answers.get(ClientHasUtrNumberPage) match {
          case Some(true) =>
            EtmpIdType.UTR

          case _ =>
            if (answers.get(ClientTaxReferencePage).nonEmpty) {
              EtmpIdType.FTR
            } else {
              if (answers.get(ClientsNinoNumberPage).nonEmpty) {
                EtmpIdType.NINO
              } else {
                val message: String = "Client must have a Tax ID type"
                logger.error(message)
                val exception = new IllegalStateException(message)
                throw exception
              }
            }
        }
    }
  }
  
  private def compareEuRegistrationDetails(
                                            originalDetails: Seq[EtmpDisplayEuRegistrationDetails],
                                            amendedDetails: Seq[EtmpEuRegistrationDetails]
                                          ): Boolean = {
    
    val addedAdditionalCountries: Boolean = originalDetails.size != amendedDetails.size
    
    val hasOriginalDetailsChanged: Boolean = amendedDetails.zip(originalDetails).collect {
      case (amended, original) =>
        amended.countryOfRegistration != original.issuedBy ||
          compareTraderId(amended.traderId, original.vatNumber, original.taxIdentificationNumber) ||
          amended.tradingName != original.fixedEstablishmentTradingName ||
          amended.fixedEstablishmentAddressLine1 != original.fixedEstablishmentAddressLine1 ||
          amended.fixedEstablishmentAddressLine2 != original.fixedEstablishmentAddressLine2 ||
          amended.townOrCity != original.townOrCity ||
          amended.regionOrState != original.regionOrState ||
          amended.postcode != original.postcode
    }.contains(true)
    
    hasOriginalDetailsChanged || addedAdditionalCountries
  }
  
  private def compareTraderId(
                             traderId: TraderId,
                             maybeVatNumber: Option[String],
                             maybeEuTaxReferenceNumber: Option[String]
                             ): Boolean = {
    traderId match {
      case vatNumberTraderId: VatNumberTraderId =>
        maybeVatNumber match {
          case Some(vatNumber) =>
            vatNumberTraderId.vatNumber != vatNumber
            
          case _ =>
            true
        }
      case taxRefTraderID: TaxRefTraderID =>
        maybeEuTaxReferenceNumber match {
          case Some(euTaxReference) =>
            taxRefTraderID.taxReferenceNumber != euTaxReference
            
          case _ =>
            true
        }
    }
  }
}