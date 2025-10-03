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

package services

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.{AmendRegistrationResultResponse, RegistrationResultResponse}
import logging.Logging
import models.domain.PreviousSchemeDetails
import models.{BusinessContactDetails, Country, InternationalAddress, TradingName, UserAnswers}
import models.etmp.amend.EtmpAmendRegistrationRequest._
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import models.etmp.display.EtmpDisplayRegistration
import models.etmp.{EtmpOtherAddress, EtmpPreviousEuRegistrationDetails, EtmpTradingName}
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplaySchemeDetails, RegistrationWrapper}
import models.previousRegistrations.PreviousRegistrationDetails
import models.vatEuDetails.{EuDetails, RegistrationType, TradingNameAndBusinessAddress}
import pages.{BusinessBasedInUKPage, BusinessContactDetailsPage, ClientBusinessAddressPage}
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.vatEuDetails.HasFixedEstablishmentPage
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import models.etmp.amend.EtmpAmendRegistrationRequest
import services.etmp.EtmpEuRegistrations
import uk.gov.hmrc.http.HeaderCarrier
import utils.CheckUkBased.isUkBasedIntermediary

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.Future
import scala.util.Try

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with Logging {

  def createRegistration(answers: UserAnswers)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    val commencementDate = LocalDate.now(clock)
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, commencementDate))
  }

  def amendRegistration(answers: UserAnswers,
                        registration: EtmpDisplayRegistration,
                        commencementDate: LocalDate,
                        intermediaryNumber: String,
                        rejoin: Boolean = false) (implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    registrationConnector.amendRegistration(
      buildEtmpAmendRegistrationRequest(
        answers = answers,
        registration = registration,
        commencementDate = commencementDate,
        intermediaryNumber = intermediaryNumber,
        rejoin = rejoin
      )
    )
  }
  
  def toUserAnswers(userId: String, registrationWrapper: RegistrationWrapper): Future[UserAnswers] = {

    val etmpTradingNames: Seq[EtmpTradingName] = registrationWrapper.etmpDisplayRegistration.tradingNames
    val maybeOtherAddress: Option[EtmpOtherAddress] = registrationWrapper.etmpDisplayRegistration.otherAddress
    val schemeDetails: EtmpDisplaySchemeDetails = registrationWrapper.etmpDisplayRegistration.schemeDetails
    val maybePreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails] =
      registrationWrapper.etmpDisplayRegistration.schemeDetails.previousEURegistrationDetails

    val hasUkBasedAddress: Boolean = isUkBasedIntermediary(registrationWrapper.vatInfo)

    val userAnswers = for {
      businessBasedInUk <- UserAnswers(
        id = userId,
        vatInfo = Some(registrationWrapper.vatInfo)
      ).set(BusinessBasedInUKPage, hasUkBasedAddress)

      hasUkAddress <- if(!hasUkBasedAddress) {
        businessBasedInUk.set(ClientBusinessAddressPage, convertNonUkAddress(maybeOtherAddress))
      } else {
        Try(businessBasedInUk)
      }

      hasTradingNamesUA <- hasUkAddress.set(HasTradingNamePage, etmpTradingNames.nonEmpty)
      tradingNamesUA <- if (etmpTradingNames.nonEmpty) {
        hasTradingNamesUA.set(AllTradingNamesQuery, convertTradingNames(etmpTradingNames).toList)
      } else {
        Try(hasTradingNamesUA)
      }

      hasPreviousEuRegistrationsUA <- tradingNamesUA.set(PreviouslyRegisteredPage, maybePreviousEuRegistrationDetails.exists(_.registrationNumber.nonEmpty))
      previousEuRegistrationsUA <- if (maybePreviousEuRegistrationDetails.exists(_.registrationNumber.nonEmpty)) {
        hasPreviousEuRegistrationsUA.set(AllPreviousRegistrationsQuery, convertEtmpPreviousEuRegistrations(maybePreviousEuRegistrationDetails))
      } else {
        Try(hasPreviousEuRegistrationsUA)
      }

      hasFixedEstablishment <- previousEuRegistrationsUA.set(HasFixedEstablishmentPage, schemeDetails.euRegistrationDetails.nonEmpty)
      euFixedEstablishmentUA <- if (schemeDetails.euRegistrationDetails.nonEmpty) {
        hasFixedEstablishment.set(AllEuDetailsQuery, convertEuFixedEstablishmentDetails(schemeDetails.euRegistrationDetails).toList)
      } else {
        Try(hasFixedEstablishment)
      }

      contactDetailsUA <- euFixedEstablishmentUA.set(BusinessContactDetailsPage, getContactDetails(schemeDetails))

    } yield contactDetailsUA

    Future.fromTry(userAnswers)
  }

  private def convertNonUkAddress(maybeOtherAddress: Option[EtmpOtherAddress]): InternationalAddress = {
    maybeOtherAddress.map { otherAddress =>
      InternationalAddress(
        line1 = otherAddress.addressLine1,
        line2 = otherAddress.addressLine2,
        townOrCity = otherAddress.townOrCity,
        stateOrRegion = otherAddress.regionOrState,
        postCode = otherAddress.postcode,
        country = Some(getCountry(otherAddress.issuedBy))
      )
    }.getOrElse {
      val exception = new IllegalStateException(s"Must have A UK Address.")
      logger.error(exception.getMessage, exception)
      throw exception
    }
  }

  private def convertTradingNames(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] = {
    for {
      tradingName <- etmpTradingNames.map(_.tradingName)
    } yield TradingName(name = tradingName)
  }


  private def convertEtmpPreviousEuRegistrations(allEtmpPreviousEuRegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails]): List[PreviousRegistrationDetails] = {
    val countrySchemaDetailsMapping: Map[Country, Seq[(Country, PreviousSchemeDetails)]] =
      allEtmpPreviousEuRegistrationDetails.map { etmpPreviousEuRegistrationDetails =>
        val country = Country.fromCountryCodeUnsafe(etmpPreviousEuRegistrationDetails.issuedBy)
        val details: PreviousSchemeDetails = PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(etmpPreviousEuRegistrationDetails)

        country -> details

      }.groupBy(_._1)

    countrySchemaDetailsMapping.map { case (country, countryPreviousSchemaDetails) =>
      PreviousRegistrationDetails(previousEuCountry = country, previousSchemesDetails = countryPreviousSchemaDetails.map(_._2))
    }.toList
  }

  private def convertEuFixedEstablishmentDetails(etmpEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails]): Seq[EuDetails] = {
    for {
      etmpDisplayEuRegistrationDetails <- etmpEuRegistrationDetails
    } yield {
      EuDetails(
        euCountry = getCountry(etmpDisplayEuRegistrationDetails.issuedBy),
        hasFixedEstablishment = Some(true),
        registrationType = determineRegistrationType(
          etmpDisplayEuRegistrationDetails.vatNumber,
          etmpDisplayEuRegistrationDetails.taxIdentificationNumber
        ),
        euVatNumber = convertEuVatNumber(etmpDisplayEuRegistrationDetails.issuedBy, etmpDisplayEuRegistrationDetails.vatNumber),
        euTaxReference = etmpDisplayEuRegistrationDetails.taxIdentificationNumber,
        tradingNameAndBusinessAddress = Some(TradingNameAndBusinessAddress(
          tradingName = TradingName(etmpDisplayEuRegistrationDetails.fixedEstablishmentTradingName),
          address = InternationalAddress(
            line1 = etmpDisplayEuRegistrationDetails.fixedEstablishmentAddressLine1,
            line2 = etmpDisplayEuRegistrationDetails.fixedEstablishmentAddressLine2,
            townOrCity = etmpDisplayEuRegistrationDetails.townOrCity,
            stateOrRegion = etmpDisplayEuRegistrationDetails.regionOrState,
            postCode = etmpDisplayEuRegistrationDetails.postcode,
            country = Some(getCountry(etmpDisplayEuRegistrationDetails.issuedBy))
          )
        ))
      )
    }
  }

  private def getCountry(countryCode: String): Country = {
    Country.fromCountryCode(countryCode) match {
      case Some(country) => country
      case _ =>
        val exception = new IllegalStateException(s"Unable to find country $countryCode")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def determineRegistrationType(vatNumber: Option[String], taxIdentificationNumber: Option[String]): Option[RegistrationType] = {
    (vatNumber, taxIdentificationNumber) match {
      case (Some(_), _) => Some(RegistrationType.VatNumber)
      case _ => Some(RegistrationType.TaxId)
    }
  }

  private def convertEuVatNumber(countryCode: String, maybeVatNumber: Option[String]): Option[String] = {
    maybeVatNumber.map { vatNumber =>
      s"$countryCode$vatNumber"
    }
  }

  private def getContactDetails(schemeDetails: EtmpDisplaySchemeDetails): BusinessContactDetails = {
    BusinessContactDetails(
      fullName = schemeDetails.contactName,
      telephoneNumber = schemeDetails.businessTelephoneNumber,
      emailAddress = schemeDetails.businessEmailId
    )
  }
}
