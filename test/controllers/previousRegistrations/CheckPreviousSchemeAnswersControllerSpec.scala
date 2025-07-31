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

package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.CheckPreviousSchemeAnswersFormProvider
import models.domain.PreviousSchemeNumbers
import models.requests.DataRequest
import models.{Country, Index, PreviousScheme}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.previousRegistrations.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.previousRegistrations.AllPreviousSchemesForCountryWithOptionalVatNumberQuery
import repositories.SessionRepository
import viewmodels.previousRegistrations.PreviousSchemeSummary
import views.html.previousRegistrations.CheckPreviousSchemeAnswersView

import scala.concurrent.Future

class CheckPreviousSchemeAnswersControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints
  private val country = Country.euCountries.head
  private val formProvider = new CheckPreviousSchemeAnswersFormProvider()
  private val form = formProvider(country)
  private val mockSessionRepository = mock[SessionRepository]

  private val baseUserAnswers =
    emptyUserAnswersWithVatInfo
      .set(PreviouslyRegisteredPage, true).success.value
      .set(PreviousEuCountryPage(index), country).success.value
      .set(PreviousSchemePage(index, index), PreviousScheme.values.head).success.value
      .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("123456789")).success.value

  private lazy val checkPreviousSchemeAnswersRoute =
    controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index).url

  "CheckPreviousSchemeAnswers Controller" - {

    "must return OK and the correct view for a GET when answers are complete" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkPreviousSchemeAnswersRoute)
        val dataRequest = DataRequest(request, baseUserAnswers.id, baseUserAnswers, "SCG DUMMY INTERMEDIARY NUMBER")

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckPreviousSchemeAnswersView]

        val previousSchemes = baseUserAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(index)).get

        val lists = PreviousSchemeSummary.getSummaryLists(previousSchemes, index, country, Seq.empty, waypoints)(dataRequest, messages(application))

        status(result) `mustEqual` OK
        contentAsString(result) mustEqual view(form, waypoints, lists, index, country, canAddScheme = true)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onSubmit(waypoints, index).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseUserAnswers.set(CheckPreviousSchemeAnswersPage(index), true).success.value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual CheckPreviousSchemeAnswersPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkPreviousSchemeAnswersRoute)
            .withFormUrlEncodedBody(("value", ""))

        val dataRequest = DataRequest(request, baseUserAnswers.id, baseUserAnswers, "SCG DUMMY INTERMEDIARY NUMBER")

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CheckPreviousSchemeAnswersView]
        implicit val msgs: Messages = messages(application)

        val previousSchemes = baseUserAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(index)).get

        val lists = PreviousSchemeSummary.getSummaryLists(previousSchemes, index, country, Seq.empty, waypoints)(dataRequest, msgs)

        val result = route(application, request).value

        status(result) `mustEqual` BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, lists, index, country, canAddScheme = true)(request, implicitly).toString
      }
    }

    "must redirect to Journey Recovery for a GET if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, checkPreviousSchemeAnswersRoute)
        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onSubmit(waypoints, index).url)

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
