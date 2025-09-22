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
import models.ClientBusinessName
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import models.saveAndComeBack.TaxReferenceInformation
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientBusinessNamePage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, EmptyWaypoints}
import services.core.CoreRegistrationValidationService
import testutils.RegistrationData.stubClockAtArbitraryDate
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext

class SaveAndComeBackServiceSpec extends AnyFreeSpec with MockitoSugar with SpecBase{

  protected val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val dataRequest: DataRequest[_] = mock[DataRequest[_]]
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
        //TODO- SCG1
        pending
      }
      "should return Left(???) when the connector returns a different error" in {
        //TODO- SCG1
        pending
      }
    }
    
    ".getSavedContinueRegistrationJourneys" - { }
    
    ".getAndValidateVatTaxInfo" - {
      "Implement testing as part of VEI-506" in {
        pending
      }
    }
    ".createTaxReferenceInfoForSavedUserAnswers" - { }
    ".retrieveSingleSavedUserAnswers" - { }
  }
}
