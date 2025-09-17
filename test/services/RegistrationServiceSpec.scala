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

package services

import base.SpecBase
import connectors.RegistrationConnector
import models.domain.PreviousSchemeDetails
import models.{BusinessContactDetails, Country, InternationalAddress, TradingName, UserAnswers}
import models.etmp.*
import models.responses.UnexpectedResponseStatus
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails, RegistrationWrapper}
import models.previousRegistrations.PreviousRegistrationDetails
import models.responses.etmp.EtmpEnrolmentResponse
import models.vatEuDetails.{EuDetails, RegistrationType, TradingNameAndBusinessAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{BusinessBasedInUKPage, BusinessContactDetailsPage, ClientBusinessAddressPage}
import play.api.test.Helpers.running
import testutils.{RegistrationData, WireMockHelper}
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier
import utils.CheckUkBased.isUkBasedIntermediary
import utils.FutureSyntax.FutureOps

class RegistrationServiceSpec extends SpecBase with WireMockHelper with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".createRegistration" - {

    "must create a registration request from user answers provided and return a successful ETMP enrolment response" in {

      val etmpEnrolmentResponse: EtmpEnrolmentResponse =
        EtmpEnrolmentResponse(iossReference = arbitrary[TaxRefTraderID].sample.value.taxReferenceNumber)

      when(mockRegistrationConnector.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder(Some(basicUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {

        registrationService.createRegistration(basicUserAnswersWithVatInfo).futureValue mustBe Right(etmpEnrolmentResponse)
        verify(mockRegistrationConnector, times(1)).createRegistration(any())(any())
      }
    }
  }

  ".amendRegistration" - {

    "must return a successful AmendRegistrationResponse when connector succeeds" in {

      val amendResponse = RegistrationData.amendRegistrationResponse
      val amendRequest = RegistrationData.etmpAmendRegistrationRequest

      when(mockRegistrationConnector.amendRegistration(any())(any())) thenReturn Right(amendResponse).toFuture

      val app = applicationBuilder(Some(basicUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {

        registrationService.amendRegistration(amendRequest).futureValue mustBe Right(amendResponse)
        verify(mockRegistrationConnector, times(1)).amendRegistration(any())(any())
      }
    }

    "must return error when connector fails" in {
      val amendRequest = RegistrationData.etmpAmendRegistrationRequest
      val error = UnexpectedResponseStatus(500, "Server error")

      when(mockRegistrationConnector.amendRegistration(any())(any())) thenReturn Left (error).toFuture

      val app = applicationBuilder().build()

      running(app) {
        registrationService.amendRegistration(amendRequest).futureValue mustBe Left(error)
      }
    }
  }

  ".toUserAnswers" - {

    "must convert from RegistrationWrapper to UserAnswers" - {

      "when user is a UK based" in {

        val ukCountryCode: String = "GB"

        val ukRegistrationWrapper: RegistrationWrapper = registrationWrapper
          .copy(vatInfo = registrationWrapper.vatInfo.
            copy(desAddress = registrationWrapper.vatInfo.desAddress
              .copy(countryCode = ukCountryCode)
            ),
            etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
              .copy(otherAddress = registrationWrapper.etmpDisplayRegistration.otherAddress.map(_.copy(issuedBy = "DE")))
          )

        val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.toUserAnswers(userAnswersId, ukRegistrationWrapper).futureValue

        result `mustBe` convertedUserAnswers(ukRegistrationWrapper).copy(lastUpdated = result.lastUpdated, journeyId = result.journeyId)
      }

      "when user is NOT based in UK" in {

        val nonUkCountryCode: String = "DE"

        val nonUkRegistrationWrapper = registrationWrapper.copy(
          vatInfo = registrationWrapper.vatInfo.
            copy(desAddress = registrationWrapper.vatInfo.desAddress
              .copy(countryCode = nonUkCountryCode)
            ),
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
            .copy(otherAddress = registrationWrapper.etmpDisplayRegistration.otherAddress.map(_.copy(issuedBy = nonUkCountryCode)))
        )

        val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.toUserAnswers(userAnswersId, nonUkRegistrationWrapper).futureValue

        result `mustBe` convertedUserAnswers(nonUkRegistrationWrapper).copy(lastUpdated = result.lastUpdated, journeyId = result.journeyId)
      }
    }

    "must throw an Illegal State Exception when non-UK based client does not provide their address details" in {

      val nonUkCountryCode: String = "DE"

      val nonUkRegistrationWrapper = registrationWrapper.copy(
        vatInfo = registrationWrapper.vatInfo.
          copy(desAddress = registrationWrapper.vatInfo.desAddress
            .copy(countryCode = nonUkCountryCode)
          ),
        etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
          .copy(otherAddress = None)
      )

      val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

      val result = service.toUserAnswers(userAnswersId, nonUkRegistrationWrapper).failed

      whenReady(result) { exp =>
        exp mustBe a[IllegalStateException]
        exp.getMessage mustBe "Must have A UK Address."
      }
    }

    "must throw an Illegal State Exception when EU country doesn't exist" in {

      val nonUkCountryCode: String = "DE"
      val nonExistentCountryCode: String = "non-existent"

      val nonUkRegistrationWrapper = registrationWrapper.copy(
        vatInfo = registrationWrapper.vatInfo.
          copy(desAddress = registrationWrapper.vatInfo.desAddress
            .copy(countryCode = nonUkCountryCode)
          ),
        etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
          .copy(otherAddress = registrationWrapper.etmpDisplayRegistration.otherAddress.map(_.copy(issuedBy = nonExistentCountryCode)))
      )

      val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

      val result = service.toUserAnswers(userAnswersId, nonUkRegistrationWrapper).failed

      whenReady(result) { exp =>
        exp mustBe a[IllegalStateException]
        exp.getMessage mustBe s"Unable to find country $nonExistentCountryCode"
      }
    }
  }

  private def convertedUserAnswers(registrationWrapper: RegistrationWrapper): UserAnswers = {

    val displayRegistration: EtmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
    val convertedTradingNamesUA: Seq[TradingName] = convertTradingNames(displayRegistration.tradingNames)
    val convertedPreviousEuRegistrationDetails: Seq[PreviousRegistrationDetails] =
      convertEtmpPreviousEuRegistrations(displayRegistration.schemeDetails.previousEURegistrationDetails)
    val convertedEuFixedEstablishmentDetails: Seq[EuDetails] =
      convertEuFixedEstablishmentDetails(displayRegistration.schemeDetails.euRegistrationDetails)
    val contactDetails: BusinessContactDetails = getContactDetails(displayRegistration.schemeDetails)

    val userAnswers = emptyUserAnswersWithVatInfo
      .copy(vatInfo = Some(registrationWrapper.vatInfo))
      .set(BusinessBasedInUKPage, isUkBasedIntermediary(registrationWrapper.vatInfo)).success.value
      .set(ClientBusinessAddressPage, convertNonUkAddress(displayRegistration.otherAddress)).success.value
      .set(HasTradingNamePage, convertedTradingNamesUA.nonEmpty).success.value
      .set(AllTradingNamesQuery, convertedTradingNamesUA.toList).success.value
      .set(PreviouslyRegisteredPage, convertedPreviousEuRegistrationDetails.nonEmpty).success.value
      .set(AllPreviousRegistrationsQuery, convertedPreviousEuRegistrationDetails.toList).success.value
      .set(HasFixedEstablishmentPage, convertedEuFixedEstablishmentDetails.nonEmpty).success.value
      .set(AllEuDetailsQuery, convertedEuFixedEstablishmentDetails.toList).success.value
      .set(BusinessContactDetailsPage, contactDetails).success.value

    if (isUkBasedIntermediary(registrationWrapper.vatInfo)) {
      userAnswers.remove(ClientBusinessAddressPage).success.value
    } else {
      userAnswers
    }
  }

  private def convertTradingNames(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] = {
    for {
      etmpTradingName <- etmpTradingNames
    } yield TradingName(name = etmpTradingName.tradingName)
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

  private def getCountry(countryCode: String): Country = {
    Country.fromCountryCode(countryCode) match {
      case Some(country) => country
      case _ =>
        val exception = new IllegalStateException(s"Unable to find country $countryCode")
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

  private def getContactDetails(schemeDetails: EtmpDisplaySchemeDetails): BusinessContactDetails = {
    BusinessContactDetails(
      fullName = schemeDetails.contactName,
      telephoneNumber = schemeDetails.businessTelephoneNumber,
      emailAddress = schemeDetails.businessEmailId
    )
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
      throw exception
    }
  }
}