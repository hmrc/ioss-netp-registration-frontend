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
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.SavedPendingRegistration
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps
import views.html.ApplicationCompleteView

class ApplicationCompleteControllerSpec extends SpecBase {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value
  private val clientName: String = savedPendingRegistration.userAnswers.vatInfo.flatMap(_.organisationName).value
  private val activationExpiryDate = savedPendingRegistration.activationExpiryDate

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(savedPendingRegistration.userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.getPendingRegistration(any())(any())) thenReturn Right(savedPendingRegistration).toFuture

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]
        
        val clientDeclarationLink: String = s"${config.clientCodeEntryUrl}${savedPendingRegistration.uniqueUrlCode}"

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(clientName, clientDeclarationLink, savedPendingRegistration.uniqueUrlCode, activationExpiryDate, config.intermediaryYourAccountUrl)(request, messages(application)).toString
        verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(savedPendingRegistration.journeyId))(any())
      }
    }

    "must throw an Exception when there is an error retrieving saved pending registration from the backend" in {

      val application = applicationBuilder(userAnswers = Some(savedPendingRegistration.userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.getPendingRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` exp.getLocalizedMessage
        }
      }
    }
  }
}
