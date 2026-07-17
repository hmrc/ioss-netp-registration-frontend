/*
 * Copyright 2026 HM Revenue & Customs
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
import models.{IntermediaryDetails, SavedPendingRegistration, SavedPendingRegistrationWithUserAnswers, UserAnswers}
import models.etmp.EtmpIdType.*
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.ClientVatNumberPage
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class PendingRegistrationServiceSpec extends SpecBase {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val pendingRegistrationWithUserAnswers: SavedPendingRegistrationWithUserAnswers =
    arbitrarySavedPendingRegistrationWithUserAnswers.arbitrary.sample.value
    .copy(intermediaryDetails = IntermediaryDetails(intermediaryNumber, intermediaryName), journeyId = journeyId)

  private val pendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value
    .copy(intermediaryDetails = IntermediaryDetails(intermediaryNumber, intermediaryName), journeyId = journeyId)

  private val anotherIntermediaryNumber = "IN9001234569"
  private val anotherIntermediaryName = "Another Intermediary Company Name"
  private val anotherPendingRegistrationWithUserAnswers: SavedPendingRegistrationWithUserAnswers =
    arbitrarySavedPendingRegistrationWithUserAnswers.arbitrary.sample.value
    .copy(intermediaryDetails = IntermediaryDetails(anotherIntermediaryNumber, anotherIntermediaryName), journeyId = journeyId)

  private val anotherPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value
    .copy(intermediaryDetails = IntermediaryDetails(anotherIntermediaryNumber, anotherIntermediaryName), journeyId = journeyId)


  "PendingRegistrationDuplicateCheckService" - {

    "when a pending registration exists for same intermediary" - {

      "must redirect to kick out page when using the same UK VAT number as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq(pendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(VRN, vrn.vrn, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, journeyId))
      }

      "must return redirect to kick out page when using the same UTR number as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq(pendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(UTR, utr, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, journeyId))
      }

      "must return redirect to kick out page when using the same NINO number as pending registration" in {

        val nino = "QQ123456C"

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq(pendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(NINO, nino, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, journeyId))
      }

      "must return redirect to kick out page when using the same client tax reference as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq(pendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(FTR, taxReference, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, journeyId))
      }
    }

    "when a pending registration exists created by another intermediary" - {

      "must redirect to kick out page when using the same UK VAT number as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn
          Right(Seq(anotherPendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(VRN, vrn.vrn, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad())
      }

      "must return redirect to kick out page when using the same UTR number as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn
          Right(Seq(anotherPendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(UTR, utr, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad())
      }

      "must return redirect to kick out page when using the same NINO number as pending registration" in {

        val nino = "QQ123456C"

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn
          Right(Seq(anotherPendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(NINO, nino, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad())
      }

      "must return redirect to kick out page when using the same client tax reference as pending registration" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn
          Right(Seq(anotherPendingRegistration)).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service.checkPendingRegistrationDuplication(FTR, taxReference, intermediaryNumber, waypoints).futureValue

        result.value mustBe Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad())
      }
    }

    "when connector returns no pending registrations" - {

      "must return None when registering with a UK VAT Number" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq.empty).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service
          .checkPendingRegistrationDuplication(VRN, vrn.vrn, intermediaryNumber, waypoints)
          .futureValue

        result mustBe None
      }

      "must return None when registering with a UTR Number" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq.empty).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service
          .checkPendingRegistrationDuplication(UTR, utr, intermediaryNumber, waypoints)
          .futureValue

        result mustBe None
      }

      "must return None when registering with a NINO" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq.empty).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service
          .checkPendingRegistrationDuplication(NINO, nino, intermediaryNumber, waypoints)
          .futureValue

        result mustBe None
      }

      "must return None when registering with a client tax reference" in {

        when(mockRegistrationConnector.getPendingRegistrationsByCustomerIdentification(any(), any())(any())) thenReturn Right(Seq.empty).toFuture

        val service = new PendingRegistrationService(mockRegistrationConnector)

        val result = service
          .checkPendingRegistrationDuplication(FTR, taxReference, intermediaryNumber, waypoints)
          .futureValue

        result mustBe None
      }
    }
  }

  "getPendingRegistration" - {

    "must return the pending registration with user answers when no VAT number is present" in {

      val savedRegistration = pendingRegistration.copy(userAnswersData = Json.obj())

      when(mockRegistrationConnector.getPendingRegistration(journeyId)(hc)) thenReturn Right(savedRegistration).toFuture

      val service = new PendingRegistrationService(mockRegistrationConnector)

      val result = service.getPendingRegistration(journeyId, userAnswersId).futureValue

      val expectedUserAnswers = UserAnswers(
        id = userAnswersId,
        journeyId = savedRegistration.journeyId,
        data = savedRegistration.userAnswersData,
        vatInfo = None,
      ).copy(lastUpdated = result.toOption.value.userAnswers.lastUpdated)

      result mustBe Right(
        SavedPendingRegistrationWithUserAnswers(
          journeyId = savedRegistration.journeyId,
          uniqueUrlCode = savedRegistration.uniqueUrlCode,
          userAnswers = expectedUserAnswers,
          lastUpdated = savedRegistration.lastUpdated,
          uniqueActivationCode = savedRegistration.uniqueActivationCode,
          intermediaryDetails = savedRegistration.intermediaryDetails
        )
      )

      verify(mockRegistrationConnector, never).getVatCustomerInfo(any())(any())
    }

    "must add VAT information when the user answers contain a VAT number" in {

      val vatInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

      val answers = UserAnswers(
        userAnswersId,
        journeyId,
        Json.obj(),
        None
      ).set(ClientVatNumberPage, vatNumber).success.value

      val savedRegistration = pendingRegistration.copy(userAnswersData = answers.data)

      when(mockRegistrationConnector.getPendingRegistration(journeyId)(hc)) thenReturn Right(savedRegistration).toFuture

      when(mockRegistrationConnector.getVatCustomerInfo(vatNumber)(hc)) thenReturn Right(vatInfo).toFuture

      val service = new PendingRegistrationService(mockRegistrationConnector)

      val result = service.getPendingRegistration(journeyId, userAnswersId).futureValue

      result.value.userAnswers.vatInfo mustBe Some(vatInfo)
    }

    "must return the VAT lookup error when VAT information cannot be retrieved" in {

      val errorResponse = InternalServerError

      val answers = UserAnswers(
        userAnswersId,
        journeyId,
        Json.obj(),
        None
      ).set(ClientVatNumberPage, vatNumber).success.value

      val savedRegistration = pendingRegistration.copy(userAnswersData = answers.data)

      when(mockRegistrationConnector.getPendingRegistration(journeyId)(hc)) thenReturn Right(savedRegistration).toFuture

      when(mockRegistrationConnector.getVatCustomerInfo(vatNumber)(hc)) thenReturn Left(errorResponse).toFuture

      val service = new PendingRegistrationService(mockRegistrationConnector)

      service
        .getPendingRegistration(journeyId, userAnswersId)
        .futureValue mustBe Left(errorResponse)
    }

    "must fail when the pending registration cannot be retrieved" in {

      val errorResponse = InternalServerError

      when(mockRegistrationConnector.getPendingRegistration(journeyId)(hc)) thenReturn Left(errorResponse).toFuture

      val service = new PendingRegistrationService(mockRegistrationConnector)

      val exception = service.getPendingRegistration(journeyId, userAnswersId).failed.futureValue

      exception mustBe a[IllegalStateException]

      exception.getMessage mustBe s"Unable to retrieve pending registration for journey ID or URL code $journeyId"
    }
  }
}
