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
import forms.previousRegistrations.PreviousOssNumberFormProvider
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.PreviousSchemeHintText
import models.{Country, CountryWithValidationDetails, Index}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.Waypoints
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.previousRegistrations.PreviousOssNumberView

class PreviousOssNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val index = Index(0)
  private val country = Country("SI", "Slovenia")
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == "SI").value
  private val formProvider = new PreviousOssNumberFormProvider()
  private val form = formProvider(country, Seq.empty)

  private def previousOssNumberRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(waypoints, index, index).url

  private val baseAnswers = emptyUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  "PreviousOssNumber Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[PreviousOssNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form, waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = baseAnswers.set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form.fill("answer"), waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .build()

      running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, index, index, countryWithValidation,
            PreviousSchemeHintText.Both)(request, messages(application)).toString

      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
