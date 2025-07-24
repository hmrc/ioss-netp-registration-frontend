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

package services.core

import base.SpecBase
import connectors.core.ValidateCoreRegistrationConnector
import models.{BankDetails, Bic, Country, DesAddress, Iban, PreviousScheme}
import models.core.{CoreRegistrationValidationResult, Match, MatchType, TraderId}
import models.iossRegistration.{IossEtmpBankDetails, IossEtmpDisplayRegistration, IossEtmpDisplaySchemeDetails, IossEtmpExclusion, IossEtmpExclusionReason, IossEtmpTradingName}
import models.ossRegistration.*
import models.requests.DataRequest
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.ioss.IossRegistrationService
import services.oss.OssRegistrationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreRegistrationValidationServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with Matchers with BeforeAndAfterEach {

  private val genericMatch = Match(
    MatchType.FixedEstablishmentActiveNETP,
    TraderId("333333333"),
    None,
    "EE",
    Some(2),
    None,
    None,
    None,
    None
  )

  private val coreValidationResponses: CoreRegistrationValidationResult =
    CoreRegistrationValidationResult(
      "333333333",
      None,
      "EE",
      traderFound = true,
      Seq(
        genericMatch
      ))

  private val connector = mock[ValidateCoreRegistrationConnector]
  private val iossRegistrationService = mock[IossRegistrationService]
  private val ossRegistrationService = mock[OssRegistrationService]


  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, emptyUserAnswers)

  implicit val dataRequest: DataRequest[AnyContent] =
    DataRequest(request, vrn.vrn, emptyUserAnswers)

  "coreRegistrationValidationService.searchUkVrn" - {

    "call searchUkVrn for any matchType and return match data" in {

      val vrn = Vrn("333333333")

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchUkVrn(vrn).futureValue.get

      value equals genericMatch
    }

    "must return None when no active match found" in {

      val vrn = Vrn("333333333")

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchUkVrn(vrn).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val vrn = Vrn("333333333")

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val response = intercept[Exception](coreRegistrationValidationService.searchUkVrn(vrn).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchEuTaxId" - {

    "call searchEuTaxId with correct Tax reference number and must return match data" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue.get

      value equals genericMatch
    }

    "must return None when no match found" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val taxRefNo: String = "333333333"
      val countryCode: String = "DE"

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val response = intercept[Exception](coreRegistrationValidationService.searchEuTaxId(taxRefNo, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchEuVrn" - {

    "call searchEuTaxId with correct EU VRN and must return match data" in {

      val euVrn: String = "333333333"
      val countrycode: String = "DE"

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchEuVrn(euVrn, countrycode).futureValue.get

      value equals genericMatch
    }

    "must return None when no match found" in {

      val euVrn: String = "333333333"
      val countryCode: String = "DE"

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val euVrn: String = "333333333"
      val countryCode: String = "DE"

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val response = intercept[Exception](coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }

  "coreRegistrationValidationService.searchScheme" - {

    "call searchScheme with correct ioss number and must return match data" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue.get

      value equals genericMatch
    }

    "call searchScheme with correct ioss number with intermediary and must return match data" in {

      val iossNumber: String = "IM333222111"
      val intermediaryNumber: String = "IN555444222"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(coreValidationResponses))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, Some(intermediaryNumber), countryCode).futureValue.get

      value equals genericMatch
    }

    "must return None when no match found" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Right(expectedResponse))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val value = coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: PreviousScheme = PreviousScheme.OSSU

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(errorCode, "error")))

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val response = intercept[Exception](coreRegistrationValidationService.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }

    "must return IOSS Match when countryCode is XI and previousScheme is IOSS" in {
      val iossNumber = "IM900123456789"
      val countryCode = Country.northernIreland.code
      val previousScheme = PreviousScheme.IOSSWOI

      val exclusion = IossEtmpExclusion(
        exclusionReason = IossEtmpExclusionReason.FailsToComply,
        decisionDate = LocalDate.of(2022, 1, 1),
        effectiveDate = LocalDate.of(2022, 2, 1),
        quarantine = true
      )

      val iossDisplayRegistration = IossEtmpDisplayRegistration(
        tradingNames = Seq(IossEtmpTradingName("test 1")),
        schemeDetails = IossEtmpDisplaySchemeDetails(
          contactName = "Test Trader",
          businessTelephoneNumber = "123456",
          businessEmailId = "test@example.com"
        ),
        bankDetails = IossEtmpBankDetails(
          accountName = "Test Account",
          iban = Iban("GB33BUKB20201555555555").value,
          bic = Some(Bic("ABCDGB2A").get)
        ),
        exclusions = Seq(exclusion)
      )

      when(iossRegistrationService.getIossRegistration(Some(iossNumber)))
        .thenReturn(Future.successful(Some(iossDisplayRegistration)))

      when(connector.validateCoreRegistration(any())(any()))
        .thenReturn(Future.successful(Right(CoreRegistrationValidationResult(
          searchId = iossNumber,
          searchIntermediary = None,
          searchIdIssuedBy = countryCode,
          traderFound = true,
          matches = Seq.empty
        ))))

      val expectedMatch = Match(
        matchType = MatchType.TraderIdQuarantinedNETP,
        traderId = TraderId(iossNumber),
        intermediary = None,
        memberState = countryCode,
        exclusionStatusCode = Some(4),
        exclusionDecisionDate = Some("2022-01-01"),
        exclusionEffectiveDate = Some("2022-02-01"),
        nonCompliantReturns = None,
        nonCompliantPayments = None
      )

      val service = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val result = service.searchScheme(iossNumber, previousScheme, None, countryCode).futureValue

      result mustBe Some(expectedMatch)
    }

    "must return OSS Match when countryCode is XI and previousScheme is OSS" in {

      val vrn = "600000014"
      val countryCode = Country.northernIreland.code
      val previousScheme = PreviousScheme.OSSU

      val exclusion = OssExcludedTrader(
        vrn = Vrn(vrn),
        exclusionReason = Some(ExclusionReason.FailsToComply),
        effectiveDate = Some(LocalDate.of(2022, 2, 1)),
        quarantined = Some(true)
      )

      val ossDisplayRegistration = OssRegistration(
        vrn = Vrn(vrn),
        registeredCompanyName = "Company Name",
        tradingNames = Seq("Trade1", "Trade2"),
        vatDetails =  mock[OssVatDetails],
        euRegistrations = Seq.empty,
        contactDetails = OssContactDetails("Test name", "0123456789", "test@test.com"),
        websites = Seq("https://example.com"),
        commencementDate = LocalDate.now(),
        previousRegistrations = Seq.empty,
        bankDetails = BankDetails("Test name", None, Iban("GB33BUKB20201555555555").value),
        isOnlineMarketplace = false,
        niPresence = None,
        dateOfFirstSale = Some(LocalDate.now()),
        submissionReceived = Some(Instant.now()),
        lastUpdated = Some(Instant.now()),
        excludedTrader = Some(exclusion),
        transferringMsidEffectiveFromDate = None,
        nonCompliantReturns = None,
        nonCompliantPayments = None,
        adminUse = mock[OssAdminUse]
      )

      when(ossRegistrationService.getLatestOssRegistration(Some(Vrn(vrn))))
        .thenReturn(Future.successful(Some(ossDisplayRegistration)))

      when(connector.validateCoreRegistration(any())(any()))
        .thenReturn(Future.successful(Right(CoreRegistrationValidationResult(
          searchId = vrn,
          searchIntermediary = None,
          searchIdIssuedBy = countryCode,
          traderFound = true,
          matches = Seq.empty
        ))))

      val expectedMatch = Match(
        matchType = MatchType.TraderIdQuarantinedNETP,
        traderId = TraderId(vrn),
        intermediary = None,
        memberState = countryCode,
        exclusionStatusCode = Some(4),
        exclusionDecisionDate = None,
        exclusionEffectiveDate = Some("2022-02-01"),
        nonCompliantReturns = None,
        nonCompliantPayments = None
      )

      val service = new CoreRegistrationValidationService(connector, iossRegistrationService, ossRegistrationService)

      val result = service.searchScheme(vrn, previousScheme, None, countryCode).futureValue

      result mustBe Some(expectedMatch)

    }
  }

}
