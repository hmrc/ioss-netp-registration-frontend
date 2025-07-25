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
import models.emails.EmailSendingResult.EMAIL_NOT_SENT
import models.responses.UnexpectedResponseStatus
import models.{BusinessContactDetails, ClientBusinessName, SavedPendingRegistration}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, ClientVatNumberPage, DeclarationPage, EmptyWaypoints, ErrorSubmittingPendingRegistrationPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.EmailService
import utils.FutureSyntax.FutureOps
import views.html.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints
  private val intermediaryCompanyName: String = intermediaryVatCustomerInfo.organisationName.get
  private val clientBusinessName: ClientBusinessName = ClientBusinessName(vatCustomerInfo.organisationName.value)
  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

  private val userAnswers = emptyUserAnswersWithVatInfo
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(ClientVatNumberPage, vatNumber).success.value
    .set(BusinessContactDetailsPage, businessContactDetails).success.value

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()


  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "Declaration Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
          .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DeclarationController.onPageLoad(waypoints).url)

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
          val request = FakeRequest(GET, routes.DeclarationController.onPageLoad(waypoints).url)

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), waypoints, intermediaryCompanyName, clientBusinessName.name)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.DeclarationController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".onSubmit" - {

      "must submit client registration, send client email, and redirect to next page when valid data and true declaration is submitted" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockEmailService = mock[EmailService]

        val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value

        val savedPendingRegWithUserAnswers: SavedPendingRegistration = savedPendingRegistration.copy(
          userAnswers = userAnswers
        )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector),
              bind[EmailService].toInstance(mockEmailService)
            )
            .build()

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any())) thenReturn Right(intermediaryVatCustomerInfo).toFuture

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Right(savedPendingRegWithUserAnswers).toFuture

        when(mockEmailService.sendClientActivationEmail(any, any, any, any, any)(any, any)) thenReturn Future.successful(())


        running(application) {
          val request =
            FakeRequest(POST, routes.DeclarationController.onSubmit(waypoints).url)
              .withFormUrlEncodedBody(("declaration", "true"))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value mustBe DeclarationPage.navigate(waypoints, userAnswers, userAnswers).route.url
          verify(mockRegistrationConnector, times(1)).submitPendingRegistration(eqTo(userAnswers))(any())
          verify(mockEmailService, times(1)).sendClientActivationEmail(
            any,
            any,
            any,
            any,
            any
          )(any, any)
        }
      }

      "must submit client registration and redirect to next page with valid data when client email fails to send" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockEmailService = mock[EmailService]

        val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value

        val savedPendingRegWithUserAnswers: SavedPendingRegistration = savedPendingRegistration.copy(
          userAnswers = userAnswers
        )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector),
              bind[EmailService].toInstance(mockEmailService)
            )
            .build()

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any())) thenReturn Right(intermediaryVatCustomerInfo).toFuture

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Right(savedPendingRegWithUserAnswers).toFuture

        when(mockEmailService.sendClientActivationEmail(any, any, any, any, any)(any, any)) thenReturn Future.successful(EMAIL_NOT_SENT)


        running(application) {
          val request =
            FakeRequest(POST, routes.DeclarationController.onSubmit(waypoints).url)
              .withFormUrlEncodedBody(("declaration", "true"))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value mustBe DeclarationPage.navigate(waypoints, userAnswers, userAnswers).route.url
          verify(mockRegistrationConnector, times(1)).submitPendingRegistration(eqTo(userAnswers))(any())
          verify(mockEmailService, times(1)).sendClientActivationEmail(
            any,
            any,
            any,
            any,
            any
          )(any, any)
        }
      }

      "must return a Bad Request and errors when the declaration data is invalid and submitted" in {
        val mockSessionRepository = mock[SessionRepository]
        val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value

        val savedPendingRegWithUserAnswers: SavedPendingRegistration = savedPendingRegistration.copy(
          userAnswers = userAnswers
        )

        when(mockRegistrationConnector.getIntermediaryVatCustomerInfo()(any()))
          .thenReturn(Future.successful(Right(intermediaryVatCustomerInfo)))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Right(savedPendingRegWithUserAnswers).toFuture

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.DeclarationController.onSubmit(waypoints).url)
              .withFormUrlEncodedBody(("declaration", ""))

          val boundForm = form.bind(Map("declaration" -> ""))

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, intermediaryCompanyName, clientBusinessName.name)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no user answer data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, routes.DeclarationController.onSubmit(waypoints).url)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the correct page when an error is returned from the backend" in {

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Left(UnexpectedResponseStatus(500, "Generic Error from database")).toFuture


        running(application) {
          val request = FakeRequest(POST, routes.DeclarationController.onSubmit(waypoints).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ErrorSubmittingPendingRegistrationPage.route(waypoints).url
        }
      }
    }
  }
}
