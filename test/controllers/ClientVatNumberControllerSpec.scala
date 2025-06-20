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
import forms.ClientVatNumberFormProvider
import models.UserAnswers
import models.responses.{InternalServerError, VatCustomerNotFound}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientVatNumberPage, EmptyWaypoints, UkVatNumberNotFoundPage, VatApiDownPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.ClientVatNumberView

import java.time.LocalDate
import scala.concurrent.Future

class ClientVatNumberControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  private val ukVatNumber: String = "123456789"

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val formProvider = new ClientVatNumberFormProvider()
  val form: Form[String] = formProvider()

  private lazy val clientVatNumberRoute: String = routes.ClientVatNumberController.onPageLoad(waypoints).url
  private lazy val clientVatNumberSubmitRoute: String = routes.ClientVatNumberController.onSubmit(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "ClientVatNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientVatNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientVatNumberView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ClientVatNumberPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientVatNumberRoute)

        val view = application.injector.instanceOf[ClientVatNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill("answer"), waypoints)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

      val vatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockRegistrationConnector.getVatCustomerInfo(any())(any())) thenReturn Future.successful(Right(vatCustomerInfo))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberRoute)
            .withFormUrlEncodedBody(("value", ukVatNumber))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers
          .copy(vatInfo = Some(vatCustomerInfo))
          .set(ClientVatNumberPage, ukVatNumber)
          .success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` ClientVatNumberPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        verify(mockRegistrationConnector, times(1)).getVatCustomerInfo(eqTo(ukVatNumber))(any())
      }
    }

    "must not save the answers and redirect to the ExpiredVrnController when deregistrationDate" in {

      val vatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value
      val expiredVrnVatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate)))

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockRegistrationConnector.getVatCustomerInfo(any())(any())) thenReturn Future.successful(Right(expiredVrnVatInfo))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberRoute)
            .withFormUrlEncodedBody(("value", ukVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.routes.ExpiredVrnDateController.onPageLoad(waypoints).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must not save any answers and redirect to Uk Vat Number Not Found page when no vat info for the users VRN is found" in {

      val mockSessionRepository = mock[SessionRepository]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockSessionRepository.set(any())) thenReturn false.toFuture
      when(mockRegistrationConnector.getVatCustomerInfo(any())(any())) thenReturn Left(VatCustomerNotFound).toFuture

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", ukVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` UkVatNumberNotFoundPage.route(waypoints).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must not save any answers and redirect to Vat Api Down page when VAT API is unreachable" in {

      val mockSessionRepository = mock[SessionRepository]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockSessionRepository.set(any())) thenReturn false.toFuture
      when(mockRegistrationConnector.getVatCustomerInfo(any())(any())) thenReturn Left(InternalServerError).toFuture

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberSubmitRoute)
            .withFormUrlEncodedBody(("value", ukVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe VatApiDownPage.route(waypoints).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClientVatNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientVatNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientVatNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
