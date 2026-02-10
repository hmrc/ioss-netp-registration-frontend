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
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckExcludedIntermediaryFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(
               registrationConnector: RegistrationConnector,
               isInAmendMode: Boolean
               ) extends CheckExcludedIntermediaryFilter(registrationConnector, isInAmendMode) {

    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  ".filter" - {

    "if an intermediary number is present in the request" - {

      "must return None" - {

        "when the amend mode is true" in {

          val app = applicationBuilder(None)
            .build()

          running(app) {

            val request = OptionalDataRequest(
              request = FakeRequest(),
              userId = userAnswersId,
              userAnswers = None,
              intermediaryNumber = Some(intermediaryNumber),
              enrolments = enrolments,
              registrationWrapper = None
            )

            val controller = new Harness(mockRegistrationConnector, isInAmendMode = true)

            val result = controller.callFilter(request).futureValue

            result must not be defined
            verifyNoInteractions(mockRegistrationConnector)
          }
        }

        "when the intermediary is not excluded" in {

          val nonExcludedIntermediary = arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
            etmpDisplayRegistration = arbitraryEtmpDisplayIntermediaryRegistration.arbitrary.sample.value.copy(
              exclusions = Seq.empty
            )
          )

          when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
            Right(nonExcludedIntermediary).toFuture

          val app = applicationBuilder(None)
            .build()

          running(app) {

            val request = OptionalDataRequest(
              request = FakeRequest(),
              userId = userAnswersId,
              userAnswers = None,
              intermediaryNumber = Some(intermediaryNumber),
              enrolments = enrolments,
              registrationWrapper = None
            )

            val controller = new Harness(mockRegistrationConnector, isInAmendMode = false)

            val result = controller.callFilter(request).futureValue

            result must not be defined
            verify(mockRegistrationConnector, times(1)).displayIntermediaryRegistration(any())(any())
          }
        }
      }

      "must redirect to CannotUseNotAnIntermediary page, when the intermediary is excluded" in {

        val excludedIntermediary = arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
          etmpDisplayRegistration = arbitraryEtmpDisplayIntermediaryRegistration.arbitrary.sample.value.copy(
            exclusions = Seq(arbitraryEtmpExclusion.arbitrary.sample.value)
          )
        )

        when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
          Right(excludedIntermediary).toFuture

        val app = applicationBuilder(None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(app) {

          val request = OptionalDataRequest(
            request = FakeRequest(),
            userId = userAnswersId,
            userAnswers = None,
            intermediaryNumber = Some(intermediaryNumber),
            enrolments = enrolments,
            registrationWrapper = None
          )

          val controller = new Harness(mockRegistrationConnector, isInAmendMode = false)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(controllers.routes.CannotUseNotAnIntermediaryController.onPageLoad().url))
          verify(mockRegistrationConnector, times(1)).displayIntermediaryRegistration(any())(any())
        }
      }
    }

    "if an intermediary number is not present in the request" - {

      "must return Unauthorised" in {

        val app = applicationBuilder(None)
          .build()

        running(app) {

          val request = OptionalDataRequest(
            request = FakeRequest(),
            userId = userAnswersId,
            userAnswers = None,
            intermediaryNumber = None,
            enrolments = enrolments,
            registrationWrapper = None
          )

          val controller = new Harness(mockRegistrationConnector, isInAmendMode = false)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Unauthorized
        }
      }
    }
  }
}

