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
import forms.DeclarationFormProvider
import models.ClientBusinessName
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientBusinessNamePage, ClientVatNumberPage, DeclarationPage, EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val intermediaryCompanyName: String = intermediaryVatCustomerInfo.organisationName.get
  private val clientBusinessName: ClientBusinessName = ClientBusinessName(vatCustomerInfo.organisationName.value)

  private val userAnswers = emptyUserAnswersWithVatInfo
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(ClientVatNumberPage, vatNumber).success.value

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val declarationRoute: String = routes.DeclarationController.onPageLoad(waypoints).url

  "Declaration Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
        .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeclarationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, intermediaryCompanyName, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
        .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))

      val answers = userAnswers.set(DeclarationPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val view = application.injector.instanceOf[DeclarationView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), waypoints, intermediaryCompanyName, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
        .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))


      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, declarationRoute)
            .withFormUrlEncodedBody(("declaration", "true"))

        val result = route(application, request).value
        val expectedAnswers = userAnswers.set(DeclarationPage, true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe DeclarationPage.navigate(waypoints, userAnswers, expectedAnswers).route.url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
        .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, declarationRoute)
            .withFormUrlEncodedBody(("declaration", ""))

        val boundForm = form.bind(Map("declaration" -> ""))

        val view = application.injector.instanceOf[DeclarationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, intermediaryCompanyName, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, declarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, declarationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
