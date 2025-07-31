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
import controllers.{clientDeclarationJourney, routes}
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import models.domain.VatCustomerInfo
import models.{ClientBusinessName, IntermediaryStuff, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientDeclarationPage
import pages.{ClientBusinessNamePage, EmptyWaypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.IntermediaryStuffQuery
import repositories.SessionRepository
import views.html.clientDeclarationJourney.ClientDeclarationView

import scala.concurrent.Future

class ClientDeclarationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach{

  val testClientName: String = "Client Company"

  val arbitraryVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value
  val testIntermediaryName: String = arbitraryVatInfo.organisationName.getOrElse("")

  lazy val clientDeclarationOnPageLoad: String = clientDeclarationJourney.routes.ClientDeclarationController.onPageLoad(waypoints).url
  lazy val clientDeclarationOnSubmit: String = clientDeclarationJourney.routes.ClientDeclarationController.onSubmit(waypoints).url

  val testUserAnswers: UserAnswers = emptyUserAnswers
    .set(ClientBusinessNamePage, ClientBusinessName(testClientName))
    .success.value

  val userAnswersWithIntermediaryStuff: UserAnswers = testUserAnswers
    .set(IntermediaryStuffQuery, IntermediaryStuff("IM Num SCG", testIntermediaryName))
    .success.value

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = reset(mockRegistrationConnector)

  val formProvider = new ClientDeclarationFormProvider()
  val form = formProvider()

  "ClientDeclaration Controller" - {

    ".onPageLoad() - GET must " - {
      "return OK and the correct view for a GET" in {
//TODO- SCG- Test is failing as no longer a mock connector call, instead need to enrich with more User Answer data to avoid Journey Recovery page!

        val application = applicationBuilder(userAnswers = Some(userAnswersWithIntermediaryStuff)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientDeclarationView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, waypoints, testClientName, testIntermediaryName)(request, messages(application)).toString
        }
      }

      "return OK and the correct view for a GET when the question has previously been answered" in {

        val filledUserAnswers = userAnswersWithIntermediaryStuff
          .set(ClientDeclarationPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(filledUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val view = application.injector.instanceOf[ClientDeclarationView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), waypoints, testClientName, testIntermediaryName)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing userAnswer data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

    ".onSubmit() - POST must" - {
      "redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswersWithIntermediaryStuff))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("declaration", "true"))

          val result = route(application, request).value

          val expectedAnswers = userAnswersWithIntermediaryStuff.set(ClientDeclarationPage, true).success.value


          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ClientDeclarationPage.navigate(EmptyWaypoints, userAnswersWithIntermediaryStuff, expectedAnswers).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {


        val application =
          applicationBuilder(userAnswers = Some(userAnswersWithIntermediaryStuff))
            .overrides(
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[ClientDeclarationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, testClientName, testIntermediaryName)(request, messages(application)).toString
        }
      }


      "redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("value[0]", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

  }
}
