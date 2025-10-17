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

package controllers.actions

import base.SpecBase
import connectors.RegistrationConnector
import models.requests.{DataRequest, OptionalDataRequest}
import models.responses.NotFound
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.IossNumberQuery
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRequiredActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  class Harness(isInAmendMode: Boolean) extends DataRequiredActionImpl(mockRegistrationConnector, isInAmendMode) {
    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "DataRequiredAction" - {

    "when isInAmendMode is false" - {

      "must return DataRequest with no registrationWrapper when userAnswers present" in {

        val action = new Harness(isInAmendMode = false)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(emptyUserAnswersWithVatInfo),
          Some(intermediaryNumber)
        )

        val result = action.callRefine(request).futureValue

        result mustBe Right(DataRequest(
          request = request.request,
          userId = userAnswersId,
          userAnswers = emptyUserAnswersWithVatInfo,
          intermediaryNumber = intermediaryNumber,
          iossNumber = None,
          registrationWrapper = None
        ))

        verifyNoInteractions(mockRegistrationConnector)
      }

      "must redirect to JourneyRecovery when no userAnswers" in {

        val action = new Harness(isInAmendMode = false)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          None,
          Some(intermediaryNumber)
        )

        val result = action.callRefine(request).futureValue

        result.isLeft mustBe true
        val redirect = result.left.getOrElse(fail("Expected redirect"))
        status(Future.successful(redirect)) mustBe SEE_OTHER

        verifyNoInteractions(mockRegistrationConnector)
      }

      "must throw exception when intermediaryNumber is missing" in {

        val action = new Harness(isInAmendMode = false)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(emptyUserAnswersWithVatInfo),
          None
        )

        val exception = intercept[IllegalStateException] {
          action.callRefine(request).futureValue
        }

        exception.getMessage must include("Intermediary Number is required")
      }
    }

    "when isInAmendMode is true" - {

      "must fetch NETP registration when IossNumberQuery is present" in {

        val userAnswersWithIossNumber = emptyUserAnswersWithVatInfo
          .set(IossNumberQuery, iossNumber).success.value

        when(mockRegistrationConnector.displayRegistrationNetp(eqTo(iossNumber))(any()))
          .thenReturn(Right(registrationWrapper).toFuture)

        val action = new Harness(isInAmendMode = true)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(userAnswersWithIossNumber),
          Some(intermediaryNumber)
        )

        val result = action.callRefine(request).futureValue

        result mustBe Right(DataRequest(
          request = request.request,
          userId = userAnswersId,
          userAnswers = userAnswersWithIossNumber,
          intermediaryNumber = intermediaryNumber,
          iossNumber = Some(iossNumber),
          registrationWrapper = Some(registrationWrapper)
        ))

        verify(mockRegistrationConnector, times(1))
          .displayRegistrationNetp(eqTo(iossNumber))(any())
        verify(mockRegistrationConnector, times(0))
          .displayRegistrationIntermediary(any())(any())
      }

      "must fetch intermediary registration when IossNumberQuery is NOT present" in {

        val userAnswersWithoutIossNumber = emptyUserAnswersWithVatInfo

        when(mockRegistrationConnector.displayRegistrationIntermediary(eqTo(intermediaryNumber))(any()))
          .thenReturn(Right(registrationWrapper).toFuture)

        val action = new Harness(isInAmendMode = true)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(userAnswersWithoutIossNumber),
          Some(intermediaryNumber)
        )

        val result = action.callRefine(request).futureValue

        result mustBe Right(DataRequest(
          request = request.request,
          userId = userAnswersId,
          userAnswers = userAnswersWithoutIossNumber,
          intermediaryNumber = intermediaryNumber,
          iossNumber = None,
          registrationWrapper = Some(registrationWrapper)
        ))

        verify(mockRegistrationConnector, times(1))
          .displayRegistrationIntermediary(eqTo(intermediaryNumber))(any())
        verify(mockRegistrationConnector, times(0))
          .displayRegistrationNetp(any())(any())
      }

      "must throw RuntimeException when displayRegistrationNetp fails" in {

        val userAnswersWithIossNumber = emptyUserAnswersWithVatInfo
          .set(IossNumberQuery, iossNumber).success.value

        when(mockRegistrationConnector.displayRegistrationNetp(eqTo(iossNumber))(any()))
          .thenReturn(Left(NotFound).toFuture)

        val action = new Harness(isInAmendMode = true)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(userAnswersWithIossNumber),
          Some(intermediaryNumber)
        )

        val exception = intercept[RuntimeException] {
          action.callRefine(request).futureValue
        }

        exception.getMessage must include("Failed to retrieve registration whilst in amend mode")
      }

      "must throw RuntimeException when displayRegistrationIntermediary fails" in {

        val userAnswersWithoutIossNumber = emptyUserAnswersWithVatInfo

        when(mockRegistrationConnector.displayRegistrationIntermediary(eqTo(intermediaryNumber))(any()))
          .thenReturn(Left(NotFound).toFuture)

        val action = new Harness(isInAmendMode = true)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(userAnswersWithoutIossNumber),
          Some(intermediaryNumber)
        )

        val exception = intercept[RuntimeException] {
          action.callRefine(request).futureValue
        }

        exception.getMessage must include("Failed to retrieve registration whilst in amend mode")
      }

      "must throw exception when intermediaryNumber is missing" in {

        val action = new Harness(isInAmendMode = true)
        val request = OptionalDataRequest(
          FakeRequest(),
          userAnswersId,
          Some(emptyUserAnswersWithVatInfo),
          None
        )

        val exception = intercept[IllegalStateException] {
          action.callRefine(request).futureValue
        }

        exception.getMessage must include("Intermediary Number is required")
        verifyNoInteractions(mockRegistrationConnector)
      }
    }
  }
}