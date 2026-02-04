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

package controllers.actions

import base.SpecBase
import connectors.RegistrationConnector
import generators.EtmpModelGenerators
import models.etmp.intermediary.IntermediaryRegistrationWrapper
import models.requests.OptionalDataRequest
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.test.FakeRequest
import services.IntermediaryRegistrationService
import uk.gov.hmrc.auth.core.Enrolments
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckIntermediaryAccessFilterSpec extends SpecBase with MockitoSugar with EtmpModelGenerators {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockIntermediaryRegistrationService: IntermediaryRegistrationService = mock[IntermediaryRegistrationService]

  class Harness(
             iossNumber: Option[String],
             registrationConnector: RegistrationConnector,
             intermediaryRegistrationService: IntermediaryRegistrationService
             ) extends CheckIntermediaryAccessFilterImpl(iossNumber, registrationConnector, intermediaryRegistrationService) {

    def callFilter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = filter(request)
  }

  def intermediaryRegistrationWithClients(iossNumbers: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryEtmpDisplayIntermediaryRegistration.arbitrary.sample.value.copy(
        clientDetails = iossNumbers.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
        )
      )
  }

  ".filter" - {

    "must redirect to AccessDenied page" - {

      "when an active intermediary does not have access to an IOSS client" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = None,
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
          Right(intermediaryRegistrationWithClients(iossNumbers = Seq("ioss-not-registered-to-intermediary"))).toFuture
        when(mockIntermediaryRegistrationService.getPreviousRegistrations()(any())) thenReturn
          Seq(previousIntermediaryRegistration).toFuture

        val action = new Harness(Some(iossNumber), mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result.value mustBe Redirect(controllers.routes.AccessDeniedController.onPageLoad().url)
      }

      "when a previously registered intermediary does not have access to an IOSS client" in {

        val previousIntermediary = previousIntermediaryRegistration.copy(
          intermediaryNumber = "IN900111111"
        )

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = None,
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(eqTo("IN900111111"))(any())) thenReturn
          Right(intermediaryRegistrationWithClients(iossNumbers = Seq("ioss-not-registered-to-intermediary"))).toFuture
        when(mockIntermediaryRegistrationService.getPreviousRegistrations()(any())) thenReturn
          Seq(previousIntermediary).toFuture

        val action = new Harness(Some(iossNumber), mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result.value mustBe Redirect(controllers.routes.AccessDeniedController.onPageLoad().url)
      }
    }

    "must return None" - {

      "when an intermediary has access to an IOSS client" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = Some(emptyUserAnswersWithVatInfo),
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(eqTo(intermediaryNumber))(any())) thenReturn
          Right(intermediaryRegistrationWithClients(iossNumbers = Seq(iossNumber))).toFuture
        when(mockIntermediaryRegistrationService.getPreviousRegistrations()(any())) thenReturn
          Seq.empty.toFuture

        val action = new Harness(Some(iossNumber), mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result mustBe None
      }

      "when an intermediary is present, but the IOSS number is not provided" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = Some(emptyUserAnswersWithVatInfo),
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(eqTo(intermediaryNumber))(any())) thenReturn
          Right(intermediaryRegistrationWithClients(Seq(iossNumber))).toFuture

        val action = new Harness(iossNumber = None, mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return Unauthorised" - {

      "when an intermediary number is not present" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = Some(emptyUserAnswersWithVatInfo),
          intermediaryNumber = None,
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(eqTo(intermediaryNumber))(any())) thenReturn
          Right(intermediaryRegistrationWithClients(Seq.empty)).toFuture
        when(mockIntermediaryRegistrationService.getPreviousRegistrations()(any())) thenReturn
          Seq.empty.toFuture

        val action = new Harness(Some(iossNumber), mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result mustBe Some(Unauthorized)
      }

      "when both intermediary and ioss number are not present" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = Some(emptyUserAnswersWithVatInfo),
          intermediaryNumber = None,
          enrolments = enrolments,
          registrationWrapper = Some(registrationWrapper)
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(eqTo(intermediaryNumber))(any())) thenReturn
          Right(intermediaryRegistrationWithClients(Seq.empty)).toFuture
        when(mockIntermediaryRegistrationService.getPreviousRegistrations()(any())) thenReturn
          Seq.empty.toFuture

        val action = new Harness(iossNumber = None, mockRegistrationConnector, mockIntermediaryRegistrationService)

        val result = action.callFilter(request).futureValue

        result mustBe Some(Unauthorized)
      }
    }

    "must throw an error" - {

      "when the retrieval of Intermediary Registration details fail" in {

        val request = OptionalDataRequest(
          request = FakeRequest(),
          userId = userAnswersId,
          userAnswers = None,
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = Enrolments(Set.empty),
          registrationWrapper = None
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
          Left(InternalServerError).toFuture

        val action = Harness(Some(iossNumber), mockRegistrationConnector, mockIntermediaryRegistrationService)

        val failedResult = intercept[Exception] {
          action.callFilter(request).futureValue
        }

        failedResult.getMessage must include ("Error retrieving intermediary registration")
      }
    }
  }
}
