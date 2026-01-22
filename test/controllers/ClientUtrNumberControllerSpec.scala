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
import forms.ClientUtrNumberFormProvider
import models.UserAnswers
import models.core.TraderId
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientUtrNumberPage, EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import testutils.CreateMatchResponse.createMatchResponse
import utils.FutureSyntax.FutureOps
import views.html.ClientUtrNumberView

import java.time.{Clock, Instant, ZoneId}

class ClientUtrNumberControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  private val utr: String = "1234567890"

  private val formProvider: ClientUtrNumberFormProvider = new ClientUtrNumberFormProvider()
  private val form: Form[String] = formProvider()

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private lazy val clientUtrNumberRoute: String = routes.ClientUtrNumberController.onPageLoad(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "ClientUtrNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientUtrNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientUtrNumberView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ClientUtrNumberPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientUtrNumberRoute)

        val view = application.injector.instanceOf[ClientUtrNumberView]

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
          FakeRequest(POST, clientUtrNumberRoute)
            .withFormUrlEncodedBody(("value", utr))

        val result = route(application, request).value

        val expectedAnswers = emptyUserAnswers.set(ClientUtrNumberPage, utr).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` ClientUtrNumberPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not save the answers and redirect to Client Already Registered page when a non-intermediary user is already registered on another service with exclusion pending" in {

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
          FakeRequest(POST, clientUtrNumberRoute)
            .withFormUrlEncodedBody(("value", utr))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.ClientAlreadyRegisteredController.onPageLoad(activeRegistrationMatch.getEffectiveDate).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(utr))(any(), any())
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
          FakeRequest(POST, clientUtrNumberRoute)
            .withFormUrlEncodedBody(("value", utr))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          quarantinedIntermediaryMatch.memberState,
          quarantinedIntermediaryMatch.getEffectiveDate
        ).url
        verifyNoMoreInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(utr))(any(), any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientUtrNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClientUtrNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientUtrNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientUtrNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
