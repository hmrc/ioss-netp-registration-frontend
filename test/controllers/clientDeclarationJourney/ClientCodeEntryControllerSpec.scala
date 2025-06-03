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

package controllers.clientDeclarationJourney

import base.SpecBase
import connectors.RegistrationConnector
import controllers.clientDeclarationJourney
import forms.clientDeclarationJourney.ClientCodeEntryFormProvider
import models.{BusinessContactDetails, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientCodeEntryPage
import pages.{BusinessContactDetailsPage, EmptyWaypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.clientDeclarationJourney.ClientCodeEntryView

import scala.concurrent.Future

class ClientCodeEntryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {


  def generate6DigitCode(): String = {
    util.Random.alphanumeric.filter(_.isUpper).take(6).mkString
  }

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value
  val testClientEmail: String = businessContactDetails.emailAddress
  val testUserAnswers: UserAnswers = emptyUserAnswers.set(BusinessContactDetailsPage, businessContactDetails).success.value

  private val savedPendingRegistration: SavedPendingRegistration =
    SavedPendingRegistration(
      journeyId = testUserAnswers.journeyId,
      uniqueUrlCode = generate6DigitCode(),
      userAnswers = testUserAnswers,
      lastUpdated = testUserAnswers.lastUpdated,
      uniqueActivationCode = generate6DigitCode(),
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

      "return OK and the correct view for a GET and successful database call" in {

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()
        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientCodeEntryView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm, waypoints, testClientEmail, testUniqueUrlCode)(request, messages(application)).toString
        }
      }

      "return OK and the correct view for a GET when the question is previously answered" in {

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        val userAnswers = UserAnswers(userAnswersId)
          .set(ClientCodeEntryPage(testUniqueUrlCode), testDeclarationCode).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val view = application.injector.instanceOf[ClientCodeEntryView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm.fill(testDeclarationCode), waypoints, testClientEmail, testUniqueUrlCode)(request, messages(application)).toString
        }
      }

      "return OK and the correct view for a GET when there is no existing userAnswer" in {

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientCodeEntryOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientCodeEntryView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(clientCodeEntryForm, waypoints, testClientEmail, testUniqueUrlCode)(request, messages(application)).toString
        }
      }

    }

    ".onSubmit()- POST must" - {
      "redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Right(true)))

        val application =
          applicationBuilder(userAnswers = Some(testUserAnswers))
            .overrides(
              bind[RegistrationConnector].toInstance(mockRegistrationConnector),
              bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnSubmit)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val result = route(application, request).value

          val expectedAnswers = testUserAnswers.set(ClientCodeEntryPage(testUniqueUrlCode), testDeclarationCode).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ClientCodeEntryPage(testUniqueUrlCode).navigate(EmptyWaypoints, testUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(1)).getPendingRegistration(any())(any())
          verify(mockRegistrationConnector, times(1)).validateClientCode(any(), any())(any())
        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {


        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        val application = applicationBuilder(userAnswers = Some(testUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnSubmit)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = clientCodeEntryForm.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[ClientCodeEntryView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, testClientEmail, testUniqueUrlCode)(request, messages(application)).toString
        }
      }

      "NOT error when existing userAnswers are NOT present" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        when(mockRegistrationConnector.validateClientCode(any(), any())(any()))
          .thenReturn(Future.successful(Right(true)))

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)

          )
          .build()

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Future.successful(Right(savedPendingRegistration)))

        running(application) {
          val request =
            FakeRequest(POST, clientCodeEntryOnPageLoad)
              .withFormUrlEncodedBody(("value", testDeclarationCode))

          val result = route(application, request).value

          val expectedAnswers = testUserAnswers.set(ClientCodeEntryPage(testUniqueUrlCode), testDeclarationCode).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ClientCodeEntryPage(testUniqueUrlCode).navigate(EmptyWaypoints, testUserAnswers, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockRegistrationConnector, times(1)).getPendingRegistration(any())(any())
          verify(mockRegistrationConnector, times(1)).validateClientCode(any(), any())(any())
        }
      }

    }


  }
}
