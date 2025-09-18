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

package controllers

import base.SpecBase
import connectors.RegistrationConnector
import models.responses.InternalServerError
import models.{BusinessContactDetails, SavedPendingRegistration}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessContactDetailsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps

class ClientEmailUpdatedControllerSpec extends SpecBase with MockitoSugar {

  private val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value
  override val journeyId: String = savedPendingRegistration.journeyId

  val newEmailAddress: String = "new@email.com"

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  "ClientEmailUpdated Controller" - {

    "must return OK and the correct view for a GET" in {

      val updatedBusinessContactDetails = businessContactDetails.copy(emailAddress = newEmailAddress)

      val updatedUserAnswers = savedPendingRegistration.userAnswers
        .set(BusinessContactDetailsPage, updatedBusinessContactDetails).success.value

      val updatedPendingReg = savedPendingRegistration.copy(userAnswers = updatedUserAnswers)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
        .thenReturn(Right(updatedPendingReg).toFuture)

      running(application) {
        val request = FakeRequest(GET, routes.ClientEmailUpdatedController.onPageLoad(waypoints, journeyId).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include(newEmailAddress)
        verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(journeyId))(any())
      }
    }

    "must throw an exception and log an error when the connector fails to return a pending registration" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
        .thenReturn(Left(InternalServerError).toFuture)

      running(application) {
        val request = FakeRequest(GET, routes.ClientEmailUpdatedController.onPageLoad(waypoints, journeyId).url)

        val result = route(application, request).value

        intercept[Exception] {
          status(result)
        }
      }
    }
  }
}
