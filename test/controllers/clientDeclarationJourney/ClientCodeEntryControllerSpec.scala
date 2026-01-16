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

package controllers.clientDeclarationJourney

import base.SpecBase
import connectors.RegistrationConnector
import controllers.{clientDeclarationJourney, routes}
import forms.clientDeclarationJourney.ClientCodeEntryFormProvider
import models.responses.NotFound
import models.{BusinessContactDetails, ClientBusinessName, IntermediaryDetails, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientCodeEntryPage
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, EmptyWaypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{EmailWasSentQuery, IntermediaryDetailsQuery}
import repositories.SessionRepository
import views.html.clientDeclarationJourney.ClientCodeEntryView

import scala.concurrent.Future

class ClientCodeEntryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  
  def generate6DigitCode(): String = {
    util.Random.alphanumeric.filter(_.isUpper).take(6).mkString
  }

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value
  private val testClientEmail: String = businessContactDetails.emailAddress
  private val testClientName: String = businessContactDetails.fullName

  val incompleteUserAnswers: UserAnswers = emptyUserAnswers
    .set(ClientBusinessNamePage, ClientBusinessName(testClientName)).success.value
    .set(IntermediaryDetailsQuery, intermediaryDetails)
    .success.value

  val completeUserAnswers: UserAnswers = incompleteUserAnswers
    .set(BusinessContactDetailsPage, businessContactDetails).success.value

  val completeUserAnswersWithEmailSent: UserAnswers = completeUserAnswers
    .set(EmailWasSentQuery, true).success.value


  private val savedPendingRegistration: SavedPendingRegistration =
    SavedPendingRegistration(
      journeyId = completeUserAnswers.journeyId,
      uniqueUrlCode = generate6DigitCode(),
      userAnswers = completeUserAnswers,
      lastUpdated = completeUserAnswers.lastUpdated,
      uniqueActivationCode = generate6DigitCode(),
      intermediaryDetails = intermediaryDetails
    )

  val testUniqueUrlCode: String = savedPendingRegistration.uniqueUrlCode
  val testDeclarationCode: String = savedPendingRegistration.uniqueActivationCode

  val formProvider: ClientCodeEntryFormProvider = new ClientCodeEntryFormProvider()
  val clientCodeEntryForm: Form[String] = formProvider()

  lazy val clientCodeEntryOnPageLoad: String = clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, testUniqueUrlCode).url
  lazy val clientCodeEntryOnSubmit: String = clientDeclarationJourney.routes.ClientCodeEntryController.onSubmit(waypoints, testUniqueUrlCode).url

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = reset(mockRegistrationConnector)

  "ClientCodeEntry Controller" - {

    ".onPageLoad()- GET must" - {

      "return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .build()
        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientCodeEntryView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm, waypoints, testClientEmail, testUniqueUrlCode, false)(request, messages(application)).toString
        }
      }

      "return OK and the correct view for a GET when the user has requested a new code" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithEmailSent))
          .build()
        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientCodeEntryView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm, waypoints, testClientEmail, testUniqueUrlCode, true)(request, messages(application)).toString
        }
      }

      "return OK and the correct view for a GET when the question is previously answered" in {

        val userAnswers = completeUserAnswers
          .set(ClientCodeEntryPage(testUniqueUrlCode), testDeclarationCode).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val view = application.injector.instanceOf[ClientCodeEntryView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm.fill(testDeclarationCode), waypoints, testClientEmail, testUniqueUrlCode, false)(request, messages(application)).toString
        }
      }

      "return an Error GET when there is no existing userAnswer to retrieve the necessary information" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

    ".onSubmit()- POST must" - {

      "redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Right(true)))

        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
              bind[RegistrationConnector].toInstance(mockRegistrationConnector),
              bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnSubmit)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val result = route(application, request).value

          val expectedAnswers = completeUserAnswers.set(ClientCodeEntryPage(testUniqueUrlCode), testDeclarationCode).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ClientCodeEntryPage(testUniqueUrlCode).navigate(EmptyWaypoints, completeUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(1)).validateClientCode(any(), any())(any())
        }
      }

      "return a bad request when valid data is submitted but validation returns as false" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Right(false)))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)

          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnSubmit)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val boundForm = clientCodeEntryForm
            .bind(Map("value" -> testDeclarationCode))
            .withError("value", "clientCodeEntry.error")

          val view = application.injector.instanceOf[ClientCodeEntryView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, testClientEmail, testUniqueUrlCode, false)(request, messages(application)).toString
        }
      }

      "return a bad request when valid data is submitted but the database returns an error" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Left(NotFound)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)

          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnPageLoad)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnSubmit)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = clientCodeEntryForm.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[ClientCodeEntryView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, testClientEmail, testUniqueUrlCode, false)(request, messages(application)).toString
        }
      }

      "return an error when existing userAnswers are not present" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Right(true)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)

          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnPageLoad)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

  }
}
