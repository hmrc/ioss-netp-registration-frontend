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

package controllers

import base.SpecBase
import controllers.routes as normalRoutes
import forms.ClientsNinoNumberFormProvider
import models.UserAnswers
import models.core.TraderId
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientsNinoNumberPage, EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import testutils.CreateMatchResponse.createMatchResponse
import utils.FutureSyntax.FutureOps
import views.html.ClientsNinoNumberView

import java.time.{Clock, Instant, ZoneId}

class ClientsNinoNumberControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  private val nino: String = "QQ123456C"

  private val formProvider: ClientsNinoNumberFormProvider = new ClientsNinoNumberFormProvider()
  private val form: Form[String] = formProvider()

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private lazy val clientsNinoNumberRoute: String = routes.ClientsNinoNumberController.onPageLoad(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }
 
  "ClientsNinoNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientsNinoNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientsNinoNumberView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ClientsNinoNumberPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientsNinoNumberRoute)

        val view = application.injector.instanceOf[ClientsNinoNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill("answer"), waypoints)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchTraderId(any[String])(any(), any())) thenReturn
          None.toFuture
        
        val request =
          FakeRequest(POST, clientsNinoNumberRoute)
            .withFormUrlEncodedBody(("value", nino))

        val result = route(application, request).value
        
        val expectedAnswers = emptyUserAnswers.set(ClientsNinoNumberPage, nino).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` ClientsNinoNumberPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not save the answers and redirect to Client Already Registered page when an active non-intermediary trader is found with exclusion pending" in {

      val today: Instant = Instant.now(stubClockAtArbitraryDate)
      val todayClock: Clock = Clock.fixed(today, ZoneId.systemDefault())

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          clock = Some(todayClock)
        )
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val activeRegistrationMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionEffectiveDate = Some(today.atZone(ZoneId.systemDefault()).toLocalDate.plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchTraderId(any[String])(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

        val request =
          FakeRequest(POST, clientsNinoNumberRoute)
            .withFormUrlEncodedBody(("value", nino))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.ClientAlreadyRegisteredController.onPageLoad(activeRegistrationMatch.getEffectiveDate).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(nino))(any(), any())
      }
    }

    "must not save the answers and redirect to Other Country Excluded And Quarantined page when a quarantined intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val quarantinedIntermediaryMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionStatusCode = Some(4)
        )

        when(mockCoreRegistrationValidationService.searchTraderId(any[String])(any(), any())) thenReturn
          Some(quarantinedIntermediaryMatch).toFuture

        val request =
          FakeRequest(POST, clientsNinoNumberRoute)
            .withFormUrlEncodedBody(("value", nino))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          quarantinedIntermediaryMatch.memberState,
          quarantinedIntermediaryMatch.getEffectiveDate
        ).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(nino))(any(), any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientsNinoNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClientsNinoNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientsNinoNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientsNinoNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
