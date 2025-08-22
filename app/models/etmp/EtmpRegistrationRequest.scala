/*
 * Copyright 2023 HM Revenue & Customs
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

package models.etmp

import formats.Format.eisDateFormatter
import logging.Logging
import models.{BusinessContactDetails, Country, UserAnswers}
import pages.*
import pages.tradingNames.HasTradingNamePage
import play.api.libs.json.{Json, OFormat}
import queries.tradingNames.AllTradingNamesQuery
import queries.{AllWebsites, IntermediaryDetailsQuery}
import services.etmp.{EtmpEuRegistrations, EtmpPreviousRegistrationsRequest}

import java.time.LocalDate

final case class EtmpRegistrationRequest(
                                          administration: EtmpAdministration,
                                          customerIdentification: EtmpCustomerIdentification,
                                          tradingNames: Seq[EtmpTradingName],
                                          intermediaryDetails: Option[EtmpIntermediaryDetails],
                                          otherAddress: Option[EtmpOtherAddress],
                                          schemeDetails: EtmpSchemeDetails,
                                          bankDetails: Option[EtmpBankDetails]
                                        )

object EtmpRegistrationRequest extends EtmpEuRegistrations with EtmpPreviousRegistrationsRequest with Logging {

  implicit val format: OFormat[EtmpRegistrationRequest] = Json.format[EtmpRegistrationRequest]

  def buildEtmpRegistrationRequest(answers: UserAnswers, commencementDate: LocalDate): EtmpRegistrationRequest = {
    val customerIdentification = getCustomerIdentification(answers)
    EtmpRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSIntAddClient),
      customerIdentification = customerIdentification,
      tradingNames = getTradingNames(answers),
      intermediaryDetails = None,
      otherAddress = getOtherAddress(customerIdentification.idType, answers),
      schemeDetails = getSchemeDetails(answers, commencementDate),
      bankDetails = None
    )
  }

  private def getCustomerIdentification(answers: UserAnswers): EtmpCustomerIdentification = {
    answers.get(IntermediaryDetailsQuery).flatMap { intermediaryDetails =>
      answers.get(ClientHasVatNumberPage).flatMap {
        case true =>
          val vatNumber = answers.get(ClientVatNumberPage).getOrElse {
            val exception = new IllegalStateException("Must have a VAT number if said yes to having a VAT number")
            logger.error(exception.getMessage, exception)
            throw exception
          }
          Some(EtmpCustomerIdentification(EtmpIdType.VRN, vatNumber, intermediaryDetails.intermediaryNumber))
        case false =>
          answers.get(BusinessBasedInUKPage).flatMap {
            case true =>
              answers.get(ClientHasUtrNumberPage).map {
                case true =>
                  val utrNumber = answers.get(ClientUtrNumberPage).getOrElse {
                    val exception = new IllegalStateException("Must have a UTR number if said yes to having a UTR number")
                    logger.error(exception.getMessage, exception)
                    throw exception
                  }
                  EtmpCustomerIdentification(EtmpIdType.UTR, utrNumber, intermediaryDetails.intermediaryNumber)
                case false =>
                  val ninoNumber = answers.get(ClientsNinoNumberPage).getOrElse {
                    val exception = new IllegalStateException("Must have a Nino number if said no to having a UTR number")
                    logger.error(exception.getMessage, exception)
                    throw exception
                  }
                  EtmpCustomerIdentification(EtmpIdType.NINO, ninoNumber, intermediaryDetails.intermediaryNumber)
              }
            case false =>
              val ftrNumber = answers.get(ClientTaxReferencePage).getOrElse {
                val exception = new IllegalStateException("Must have a FTR number if said no to having a UK VAT number and not based in UK")
                logger.error(exception.getMessage, exception)
                throw exception
              }
              Some(EtmpCustomerIdentification(EtmpIdType.FTR, ftrNumber, intermediaryDetails.intermediaryNumber))
          }
      }
    }.getOrElse {
      val exception = new IllegalStateException("Must have answered on having a UK VAT number and have intermediary details")
      logger.error(exception.getMessage, exception)
      throw exception
    }
  }

  private def getOtherAddress(idType: EtmpIdType, answers: UserAnswers): Option[EtmpOtherAddress] = {
    idType match {
      case EtmpIdType.VRN =>
        None
      case _ =>
        Some(
          (for {
            basedInUK <- answers.get(BusinessBasedInUKPage)
            clientCountryBased <- idType match {
              case EtmpIdType.UTR | EtmpIdType.NINO =>
                Some(Country.northernIreland)
              case _ =>
                answers.get(ClientCountryBasedPage)
            }
            businessAddress <- answers.get(ClientBusinessAddressPage)
            businessName <- answers.get(ClientBusinessNamePage)
          } yield {

            EtmpOtherAddress(
              issuedBy = clientCountryBased.code,
              tradingName = Some(businessName.name),
              addressLine1 = businessAddress.line1,
              addressLine2 = businessAddress.line2,
              townOrCity = businessAddress.townOrCity,
              regionOrState = businessAddress.stateOrRegion,
              postcode = businessAddress.postCode
            )
          }).getOrElse {
            val exception = new IllegalStateException("Didn't have a VRN, so must havea business address, trading name and country")
            logger.error(exception.getMessage, exception)
            throw exception
          }
        )
    }
  }

  private def getSchemeDetails(answers: UserAnswers, commencementDate: LocalDate): EtmpSchemeDetails = {

    val businessContactDetails = getBusinessContactDetails(answers)

    val nonCompliantDetails = getMaximumNonCompliantDetails(answers)

    EtmpSchemeDetails(
      commencementDate = commencementDate.format(eisDateFormatter),
      euRegistrationDetails = getEuTaxRegistrations(answers),
      previousEURegistrationDetails = getPreviousRegistrationDetails(answers),
      websites = Some(getWebsites(answers)),
      contactName = businessContactDetails.fullName,
      businessTelephoneNumber = businessContactDetails.telephoneNumber,
      businessEmailId = businessContactDetails.emailAddress,
      nonCompliantReturns = nonCompliantDetails.flatMap(_.nonCompliantReturns.map(_.toString)),
      nonCompliantPayments = nonCompliantDetails.flatMap(_.nonCompliantPayments.map(_.toString))
    )
  }

  private def getTradingNames(answers: UserAnswers): List[EtmpTradingName] = {
    answers.get(HasTradingNamePage) match {
      case Some(true) =>
        answers.get(AllTradingNamesQuery) match {
          case Some(tradingNames) =>
            for {
              tradingName <- tradingNames
            } yield EtmpTradingName(tradingName = tradingName.name)
          case Some(Nil) | None =>
            val exception = new IllegalStateException("Must have at least one trading name")
            logger.error(exception.getMessage, exception)
            throw exception
        }

      case Some(false) =>
        List.empty

      case None =>
        val exception = new IllegalStateException("Must select Yes if trading name is different")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getBusinessContactDetails(answers: UserAnswers): BusinessContactDetails = {
    answers.get(BusinessContactDetailsPage) match {
      case Some(contactDetails) => contactDetails
      case _ =>
        val exception = new IllegalStateException("User must provide contact details")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getWebsites(answers: UserAnswers): List[EtmpWebsite] =
    answers.get(AllWebsites) match {
      case Some(websites) =>
        for {
          website <- websites
        } yield EtmpWebsite(websiteAddress = website.site)
      case _ =>
        val exception = new IllegalStateException("User must have at least one website")
        logger.error(exception.getMessage, exception)
        throw exception
    }

}
