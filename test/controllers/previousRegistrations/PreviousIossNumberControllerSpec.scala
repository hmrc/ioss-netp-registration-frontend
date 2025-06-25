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
import forms.previousRegistrations.PreviousIossNumberFormProvider
import models.domain.PreviousSchemeNumbers
import models.{Country, Index}
import org.scalatestplus.mockito.MockitoSugar
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.previousRegistrations.PreviousIossNumberView


class PreviousIossNumberControllerSpec extends SpecBase with MockitoSugar {

  private val index = Index(0)

  private val country = Country.euCountries.head
  private val formProvider = new PreviousIossNumberFormProvider()
  private val form = formProvider(country)

  private lazy val previousIossNumberRoute = controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(waypoints, index, index).url

  private val iossHintText = "This will start with IM040 followed by 7 numbers"

  private val baseAnswers = emptyUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  "PreviousIossNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousIossNumberView]

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form, waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form.fill("answer"), waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
