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
import forms.WebsiteFormProvider
import models.{Index, UserAnswers, Website}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.website.WebsitePage
import pages.{EmptyWaypoints, Waypoints}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.WebsiteView

import scala.concurrent.Future


class WebsiteControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)
  private val waypoints: Waypoints = EmptyWaypoints

  private val formProvider = new WebsiteFormProvider()
  private val form = formProvider(index, Seq.empty)

  private lazy val websiteRoute = controllers.website.routes.WebsiteController.onPageLoad(waypoints, index).url

  "Website Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, websiteRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WebsiteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WebsitePage(index), Website("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, websiteRoute)

        val view = application.injector.instanceOf[WebsiteView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), waypoints, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, websiteRoute)
            .withFormUrlEncodedBody(("value", "https://www.example.com"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(WebsitePage(index), Website("https://www.example.com")).success.value
        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual WebsitePage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, websiteRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WebsiteView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, index)(request, messages(application)).toString
      }
    }

    "must return NOT_FOUND for a GET with an index of position 10 or greater" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, controllers.website.routes.WebsiteController.onPageLoad(waypoints, Index(10)).url)

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }

    "must return NOT_FOUND for a POST with an index of position 10 or greater" in {

      val answers =
        emptyUserAnswers
          .set(WebsitePage(Index(0)), Website("foo1")).success.value
          .set(WebsitePage(Index(1)), Website("foo2")).success.value
          .set(WebsitePage(Index(2)), Website("foo3")).success.value
          .set(WebsitePage(Index(3)), Website("foo4")).success.value
          .set(WebsitePage(Index(4)), Website("foo5")).success.value
          .set(WebsitePage(Index(5)), Website("foo6")).success.value
          .set(WebsitePage(Index(6)), Website("foo7")).success.value
          .set(WebsitePage(Index(7)), Website("foo8")).success.value
          .set(WebsitePage(Index(8)), Website("foo9")).success.value
          .set(WebsitePage(Index(9)), Website("foo10")).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {

        val request =
          FakeRequest(POST, controllers.website.routes.WebsiteController.onSubmit(waypoints, Index(10)).url)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual NOT_FOUND
      }
    }

  }
}
