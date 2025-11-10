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

package controllers.amend

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import forms.amend.CancelAmendRegistrationFormProvider
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.ChangeRegistrationPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.SessionKeys
import views.html.amend.CancelAmendRegistrationView

import scala.concurrent.Future

class CancelAmendRegistrationControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val formProvider = new CancelAmendRegistrationFormProvider()
  private val form = formProvider()

  lazy val cancelAmendRegistrationRoute: String = controllers.amend.routes.CancelAmendRegistrationController.onPageLoad(waypoints).url

  "CancelAmendRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      when(mockRegistrationConnector.displayRegistrationNetp(any())(any()))
        .thenReturn(Future.successful(Right(registrationWrapper)))

      running(application) {
        val request = FakeRequest(GET, cancelAmendRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelAmendRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must delete the amended answers and redirect to yourAccount page when the user answers Yes" in {

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      when(mockRegistrationConnector.displayRegistrationNetp(any())(any()))
        .thenReturn(Future.successful(Right(registrationWrapper)))


      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        val sessionId = "12345-credId"
        val request =
          FakeRequest(POST, cancelAmendRegistrationRoute)
            .withSession(SessionKeys.sessionId -> sessionId)
            .withFormUrlEncodedBody(("value", "true"))

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe config.intermediaryYourAccountUrl
        verify(mockSessionRepository, times(1)).clear(eqTo(sessionId))
      }
    }

    "must NOT delete the amended answers and returns user to ChangeYourRegistration page when the user answers No" in {

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      when(mockRegistrationConnector.displayRegistrationNetp(any())(any()))
        .thenReturn(Future.successful(Right(registrationWrapper)))

      val application =
        applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        val sessionId = "12345-credId"
        val request =
          FakeRequest(POST, cancelAmendRegistrationRoute)
            .withSession(SessionKeys.sessionId -> sessionId)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe ChangeRegistrationPage.route(waypoints).url
        verify(mockSessionRepository, never()).set(eqTo(basicUserAnswersWithVatInfo))
      }
    }
  }
}
