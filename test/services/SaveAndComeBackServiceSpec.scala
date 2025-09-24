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

import base.SpecBase
import connectors.{RegistrationConnector, SaveForLaterConnector}
import models.domain.VatCustomerInfo
import models.requests.{DataRequest, OptionalDataRequest}
import models.responses.NotFound
import models.saveAndComeBack.{MultipleRegistrations, NoRegistrations, SingleRegistration, TaxReferenceInformation}
import models.{ClientBusinessName, SavedUserAnswers}
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientBusinessNamePage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, ContinueRegistrationSelectionPage, EmptyWaypoints}
import play.api.libs.json.JsObject
import services.core.CoreRegistrationValidationService
import testutils.RegistrationData.stubClockAtArbitraryDate
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.Instant
import scala.concurrent.ExecutionContext

class SaveAndComeBackServiceSpec extends AnyFreeSpec with MockitoSugar with SpecBase {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val dataRequest: DataRequest[_] = mock[DataRequest[_]]
  private implicit val optionalDataRequest: OptionalDataRequest[_] = mock[OptionalDataRequest[_]]
  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]
  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  val testSaveAndComeBackService: SaveAndComeBackService = SaveAndComeBackService(
    clock = stubClockAtArbitraryDate,
    registrationConnector = mockRegistrationConnector,
    coreRegistrationValidationService = mockCoreRegistrationValidationService,
    saveForLaterConnector = mockSaveForLaterConnector)(ec)

  "SaveAndComeBackService" - {
    ".determineTaxReference" - {
      val vatOrganisationName: String = emptyUserAnswersWithVatInfo.vatInfo.get.organisationName.get
      val vatTaxReference: String = "VAT reference"
      val vatNumber: String = "VatNum12345"
      val vatJourneyId: String = emptyUserAnswersWithVatInfo.journeyId

      "should return the correct values when a user has VAT information and a organisation name" in {
        val answers = emptyUserAnswersWithVatInfo
          .set(ClientVatNumberPage, vatNumber).success.value

        val result: TaxReferenceInformation = testSaveAndComeBackService.determineTaxReference(answers)

        val expectedResult: TaxReferenceInformation = TaxReferenceInformation(vatOrganisationName, vatTaxReference, vatNumber, vatJourneyId)

        result mustEqual expectedResult
      }

      "should return the correct values when a user has VAT information and no organisation name" in {
        val answers = emptyUserAnswersWithVatInfo.copy(
            vatInfo = Some(emptyUserAnswersWithVatInfo.vatInfo.get.copy(
              organisationName = None,
              individualName = Some("Name")
            ))
          )
          .set(ClientVatNumberPage, vatNumber).success.value

        val result: TaxReferenceInformation = testSaveAndComeBackService.determineTaxReference(answers)

        val expectedResult: TaxReferenceInformation = TaxReferenceInformation("Name", vatTaxReference, vatNumber, vatJourneyId)

        result mustEqual expectedResult
      }

      "should return an error when VAT information is present but a VAT number is not found" in {
        val answers = emptyUserAnswersWithVatInfo

        val ex = intercept[IllegalStateException] {
          testSaveAndComeBackService.determineTaxReference(answers)
        }
        ex.getMessage mustEqual "User answers must include VAT number if vatCustomerInfo present"
      }

      "should return the correct values when a user has a tax reference" in {
        val answers = emptyUserAnswers
          .set(ClientTaxReferencePage, "TaxRef123").success.value
          .set(ClientBusinessNamePage, ClientBusinessName("Client Business Name")).success.value

        val result: TaxReferenceInformation = testSaveAndComeBackService.determineTaxReference(answers)

        val expectedResult: TaxReferenceInformation = TaxReferenceInformation("Client Business Name", "tax reference", "TaxRef123", emptyUserAnswers.journeyId)

        result mustEqual expectedResult
      }

      "should return the correct values when a user has a UTR number" in {
        val answers = emptyUserAnswers
          .set(ClientUtrNumberPage, "UTR123").success.value
          .set(ClientBusinessNamePage, ClientBusinessName("Client Business Name")).success.value

        val result: TaxReferenceInformation = testSaveAndComeBackService.determineTaxReference(answers)

        val expectedResult: TaxReferenceInformation = TaxReferenceInformation("Client Business Name", "tax reference", "UTR123", emptyUserAnswers.journeyId)

        result mustEqual expectedResult
      }

      "should return the correct values when a user has a Nino reference" in {
        val answers = emptyUserAnswers
          .set(ClientsNinoNumberPage, "NINO123").success.value
          .set(ClientBusinessNamePage, ClientBusinessName("Client Business Name")).success.value

        val result: TaxReferenceInformation = testSaveAndComeBackService.determineTaxReference(answers)

        val expectedResult: TaxReferenceInformation = TaxReferenceInformation(
          "Client Business Name",
          "National Insurance Number",
          "NINO123",
          emptyUserAnswers.journeyId)

        result mustEqual expectedResult
      }

      "should return an error when VAT information is not present and a business name cannot be found" in {
        val answers = emptyUserAnswers

        val ex = intercept[IllegalStateException] {
          testSaveAndComeBackService.determineTaxReference(answers)
        }
        ex.getMessage mustEqual "User answers must include company name if Vat Customer Info was not provided"
      }

    }

    ".getVatTaxInfo" - {
      "should return Right(VatCustomerInfo) when the connector returns the data" in {
        val vatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

        when(mockRegistrationConnector.getVatCustomerInfo("VatNum1")).thenReturn(Right(vatCustomerInfo).toFuture)

        val result = testSaveAndComeBackService.getVatTaxInfo("VatNum1", EmptyWaypoints).futureValue

        val expectedResult = Right(vatCustomerInfo)
        result mustBe expectedResult
      }
      "should return Left(???) when the connector returns an error" in {
        //TODO- VEI-506
        pending
      }
      "should return Left(???) when the connector returns a different error" in {
        //TODO- VEI-506
        pending
      }
    }

    ".getSavedContinueRegistrationJourneys" - {

      val intermediaryNumber = "IM12345678"
      val savedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value
      val seqSavedUserAnswers = Seq(savedUserAnswers, savedUserAnswers, savedUserAnswers)

      "should return a SingleRegistration when a journeyId has been set in userAnswers" in {
        val answers = emptyUserAnswers
          .set(ContinueRegistrationSelectionPage, "journeyId").success.value


        val result = testSaveAndComeBackService.getSavedContinueRegistrationJourneys(answers, intermediaryNumber).futureValue

        result mustBe SingleRegistration("journeyId")
      }
      "should return a SingleRegistration when one registration is returned from the database" in {
        val answers = emptyUserAnswers

        when(mockSaveForLaterConnector.getAllByIntermediary(intermediaryNumber)).thenReturn(Right(Seq(savedUserAnswers)).toFuture)

        val result = testSaveAndComeBackService.getSavedContinueRegistrationJourneys(answers, intermediaryNumber).futureValue

        result mustBe SingleRegistration(savedUserAnswers.journeyId)
      }
      "should return a MultipleRegistration when more than one registration is returned from the database" in {
        val answers = emptyUserAnswers

        when(mockSaveForLaterConnector.getAllByIntermediary(intermediaryNumber)).thenReturn(Right(seqSavedUserAnswers).toFuture)

        val result = testSaveAndComeBackService.getSavedContinueRegistrationJourneys(answers, intermediaryNumber).futureValue

        result mustBe MultipleRegistrations(seqSavedUserAnswers)
      }
      "should return a NoRegistrations when no registrations are returned from the database" in {
        val answers = emptyUserAnswers

        when(mockSaveForLaterConnector.getAllByIntermediary(intermediaryNumber)).thenReturn(Right(Seq.empty).toFuture)

        val result = testSaveAndComeBackService.getSavedContinueRegistrationJourneys(answers, intermediaryNumber).futureValue

        result mustBe NoRegistrations
      }
      "should return an error when the connector returns an error retrieving registrations" in {
        val answers = emptyUserAnswers

        when(mockSaveForLaterConnector.getAllByIntermediary(intermediaryNumber)).thenReturn(Left(NotFound).toFuture)

        val ex = intercept[Exception] {
          testSaveAndComeBackService.getSavedContinueRegistrationJourneys(answers, intermediaryNumber).futureValue
        }

        val expectedMessage = s"The future returned an exception of type: java.lang.Exception, with message: " +
          s"Received an unexpected error when trying to retrieve uncompleted registrations for the intermediary ID: $intermediaryNumber. " +
          s"\nWith Errors: NotFound."

        ex.getMessage mustEqual expectedMessage
      }
    }

    ".getAndValidateVatTaxInfo" - {
      "Implement testing as part of VEI-506" in {
        pending
      }
    }

    ".createTaxReferenceInfoForSavedUserAnswers" - {
      val businessName = "Client Business Name"
      val nonVatTaxReferenceNumber = "TaxRef123"
      val vatNumber = "GB1234567"

      val nonVatUserAnswers = emptyUserAnswers
        .set(ClientTaxReferencePage, nonVatTaxReferenceNumber).success.value
        .set(ClientBusinessNamePage, ClientBusinessName(businessName)).success.value

      val vatUserAnswers = emptyUserAnswersWithVatInfo
        .set(ClientVatNumberPage, vatNumber).success.value
        .set(ClientTaxReferencePage, nonVatTaxReferenceNumber).success.value
        .set(ClientBusinessNamePage, ClientBusinessName(businessName)).success.value

      "Should return a Future[Seq[TaxReferenceInformation]] for each Saved User Answers when" - {
        "the user answers are NOT a VAT customer" in {

          val savedUserAnswers = SavedUserAnswers(
            journeyId = journeyId,
            data = nonVatUserAnswers.data,
            intermediaryNumber = "IM1234",
            lastUpdated = Instant.now())

          val seqSavedUserAnswers = Seq(savedUserAnswers, savedUserAnswers)

          val taxReferenceInformation = TaxReferenceInformation(
            organisationName = businessName,
            taxReference = "tax reference",
            referenceNumber = nonVatTaxReferenceNumber,
            journeyId = journeyId)


          val result = testSaveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(seqSavedUserAnswers).futureValue

          result mustBe Seq(taxReferenceInformation, taxReferenceInformation)
        }

        "the user answers ARE a VAT customer" in {

          val vatSavedUserAnswers = SavedUserAnswers(
            journeyId = journeyId,
            data = vatUserAnswers.data,
            intermediaryNumber = "IM1234",
            lastUpdated = Instant.now())

          val seqVatSavedUserAnswers = Seq(vatSavedUserAnswers)

          val vatTaxReferenceInformation = TaxReferenceInformation(
            organisationName = vatUserAnswers.vatInfo.get.organisationName.get,
            taxReference = "VAT reference",
            referenceNumber = vatNumber,
            journeyId = journeyId)

          when(mockRegistrationConnector.getVatCustomerInfo(vatNumber)).thenReturn(Right(vatUserAnswers.vatInfo.get).toFuture)

          val result = testSaveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(seqVatSavedUserAnswers).futureValue

          result mustBe Seq(vatTaxReferenceInformation)
        }

        "the user answers BOTH a VAT and NON VAT customer" in {

          val savedUserAnswers = SavedUserAnswers(
            journeyId = journeyId,
            data = nonVatUserAnswers.data,
            intermediaryNumber = "IM1234",
            lastUpdated = Instant.now())

          val taxReferenceInformation = TaxReferenceInformation(
            organisationName = businessName,
            taxReference = "tax reference",
            referenceNumber = nonVatTaxReferenceNumber,
            journeyId = journeyId)

          val vatSavedUserAnswers = SavedUserAnswers(
            journeyId = journeyId,
            data = vatUserAnswers.data,
            intermediaryNumber = "IM1234",
            lastUpdated = Instant.now())

          val vatTaxReferenceInformation = TaxReferenceInformation(
            organisationName = vatUserAnswers.vatInfo.get.organisationName.get,
            taxReference = "VAT reference",
            referenceNumber = vatNumber,
            journeyId = journeyId)

          val seqSavedUserAnswers = Seq(vatSavedUserAnswers, savedUserAnswers)


          when(mockRegistrationConnector.getVatCustomerInfo(vatNumber)).thenReturn(Right(vatUserAnswers.vatInfo.get).toFuture)

          val result = testSaveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(seqSavedUserAnswers).futureValue

          result mustBe Seq(vatTaxReferenceInformation, taxReferenceInformation)
        }
      }

      "Should return an exception when there is an error with getting the vat information" - {

        val vatSavedUserAnswers = SavedUserAnswers(
          journeyId = journeyId,
          data = vatUserAnswers.data,
          intermediaryNumber = "IM1234",
          lastUpdated = Instant.now())

        val seqVatSavedUserAnswers = Seq(vatSavedUserAnswers)

        when(mockRegistrationConnector.getVatCustomerInfo(vatNumber)).thenReturn(Left(NotFound).toFuture)

        val ex = intercept[Exception] {
          testSaveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(seqVatSavedUserAnswers).futureValue
        }

        val expectedMessage = s"The future returned an exception of type: java.lang.Exception, with message: " +
          s"Error returned from registration connector. Page to be implemented in VEI-506."

        ex.getMessage mustEqual expectedMessage
      }
    }

    ".retrieveSingleSavedUserAnswers" - {
      val testIntermediaryNum = "IM12345"
      val testUserAnswers = emptyUserAnswers.copy(id = "UserId1")
      val testSavedUserAnswers = SavedUserAnswers(journeyId = journeyId,
        data = testUserAnswers.data,
        intermediaryNumber = testIntermediaryNum,
        lastUpdated = testUserAnswers.lastUpdated)

      "should return a Future(UserAnswers) when the database returns a SavedUserAnswers instance" in {
        when(mockSaveForLaterConnector.getClientRegistration(journeyId)).thenReturn(Right(testSavedUserAnswers).toFuture)
        when(optionalDataRequest.userId).thenReturn("UserId1")

        val result = testSaveAndComeBackService.retrieveSingleSavedUserAnswers(journeyId, EmptyWaypoints)(optionalDataRequest, hc).futureValue

        result mustEqual testUserAnswers
      }

      "should return an error when the database returns an error retrieving SavedUserAnswers" in {
        when(mockSaveForLaterConnector.getClientRegistration(journeyId)).thenReturn(Left(NotFound).toFuture)

        val ex = intercept[Exception] {
          testSaveAndComeBackService.retrieveSingleSavedUserAnswers(journeyId, EmptyWaypoints)(optionalDataRequest, hc).futureValue
        }

        val expectedMessage = s"The future returned an exception of type: java.lang.Exception, with message: " +
          s"Received an unexpected error when trying to retrieve Saved User Answers for the journey ID: $journeyId," +
          s"\nWith Errors: NotFound."

        ex.getMessage mustEqual expectedMessage
      }
    }
  }
}
