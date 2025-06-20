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

package controllers.website

import base.SpecBase
import controllers.routes
import forms.AddWebsiteFormProvider
import models.{Index, Website}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.website.{AddWebsitePage, WebsitePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, *}
import repositories.SessionRepository
import viewmodels.WebsiteSummary
import views.html.AddWebsiteView

import scala.concurrent.Future

class AddWebsiteControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new AddWebsiteFormProvider()
  private val form = formProvider()

  private def addWebsiteRoute(waypoints: Waypoints = EmptyWaypoints) = controllers.website.routes.AddWebsiteController.onPageLoad(waypoints).url

  private val baseAnswers = emptyUserAnswers.set(WebsitePage(Index(0)), Website("foo")).success.value

  "AddWebsite Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val view = application.injector.instanceOf[AddWebsiteView]

        val list = WebsiteSummary.addToListRows(baseAnswers, EmptyWaypoints, AddWebsitePage())

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, EmptyWaypoints, list, canAddWebsites = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET and allow adding when just below the max website boundary" in {
      val canAddWebsites = true
      val totalAddedWebsites = 9

      val answers = (0 until totalAddedWebsites).foldLeft(emptyUserAnswers) { case (userAnswers, index) =>
        userAnswers.set(WebsitePage(Index(index)), Website("foo")).success.value
      }

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddWebsiteView]

        val list = WebsiteSummary.addToListRows(answers, EmptyWaypoints, AddWebsitePage())

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, EmptyWaypoints, list, canAddWebsites)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET and not allow adding websites when there are already max websites added" in {
      val canAddWebsites = false
      val totalAddedWebsites = 10

      val answers = (0 until totalAddedWebsites).foldLeft(emptyUserAnswers) { case (userAnswers, index) =>
        userAnswers.set(WebsitePage(Index(index)), Website("foo")).success.value
      }

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddWebsiteView]

        val list = WebsiteSummary.addToListRows(answers, EmptyWaypoints, AddWebsitePage())

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, EmptyWaypoints, list, canAddWebsites)(request, messages(application)).toString

      }
    }

    "must redirect to Website Address page for a GET when cannot derive number of websites" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.website.routes.WebsiteController.onPageLoad(EmptyWaypoints, Index(0)).url
      }
    }

    "must not populate the answer on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(AddWebsitePage(), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val view = application.injector.instanceOf[AddWebsiteView]

        val list = WebsiteSummary.addToListRows(baseAnswers, EmptyWaypoints, AddWebsitePage())

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must not be view(form.fill(true), EmptyWaypoints, list, canAddWebsites = true)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addWebsiteRoute())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(AddWebsitePage(), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual AddWebsitePage().navigate(EmptyWaypoints, expectedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addWebsiteRoute())
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddWebsiteView]

        val list = WebsiteSummary.addToListRows(baseAnswers, EmptyWaypoints, AddWebsitePage())

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, EmptyWaypoints, list, canAddWebsites = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addWebsiteRoute())

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addWebsiteRoute())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
