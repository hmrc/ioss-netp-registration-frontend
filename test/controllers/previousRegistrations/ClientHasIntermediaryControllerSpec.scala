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

package controllers.previousRegistrations

import base.SpecBase
import forms.previousRegistrations.ClientHasIntermediaryFormProvider
import models.{Index, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.previousRegistrations.ClientHasIntermediaryPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import views.html.previousRegistrations.ClientHasIntermediaryView

import scala.concurrent.Future

class ClientHasIntermediaryControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new ClientHasIntermediaryFormProvider()
  private val form: Form[Boolean] = formProvider()

  private val index: Index = Index(0)

  private lazy val clientHasIntermediaryRoute: String = routes.ClientHasIntermediaryController.onPageLoad(waypoints, index, index).url

  "ClientHasIntermediary Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientHasIntermediaryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientHasIntermediaryView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ClientHasIntermediaryPage(index, index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientHasIntermediaryRoute)

        val view = application.injector.instanceOf[ClientHasIntermediaryView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(true), waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clientHasIntermediaryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = emptyUserAnswers
          .set(ClientHasIntermediaryPage(index, index), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` ClientHasIntermediaryPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientHasIntermediaryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClientHasIntermediaryView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientHasIntermediaryRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientHasIntermediaryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
