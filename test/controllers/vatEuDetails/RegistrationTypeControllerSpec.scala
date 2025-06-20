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

package controllers.vatEuDetails

import base.SpecBase
import forms.vatEuDetails.RegistrationTypeFormProvider
import models.{Country, RegistrationType, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.{EuCountryPage, HasFixedEstablishmentPage, RegistrationTypePage, VatRegisteredInEuPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.vatEuDetails.RegistrationTypeView
import utils.FutureSyntax.FutureOps


class RegistrationTypeControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val formProvider = new RegistrationTypeFormProvider()
  private val form: Form[RegistrationType] = formProvider(country)

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(VatRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex(0)), true).success.value

  private lazy val registrationTypeRoute: String = routes.RegistrationTypeController.onPageLoad(waypoints, countryIndex(0)).url

  "RegistrationType Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registrationTypeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RegistrationTypeView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(RegistrationTypePage(countryIndex(0)), RegistrationType.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, registrationTypeRoute)

        val view = application.injector.instanceOf[RegistrationTypeView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(RegistrationType.values.head), waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, registrationTypeRoute)
            .withFormUrlEncodedBody(("value", RegistrationType.values.head.toString))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(RegistrationTypePage(countryIndex(0)), RegistrationType.values.head).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` RegistrationTypePage(countryIndex(0)).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, registrationTypeRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[RegistrationTypeView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, registrationTypeRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, registrationTypeRoute)
            .withFormUrlEncodedBody(("value", RegistrationType.values.head.toString))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
