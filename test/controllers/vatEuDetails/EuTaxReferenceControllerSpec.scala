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
import forms.vatEuDetails.EuTaxReferenceFormProvider
import models.{Country, RegistrationType, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import views.html.vatEuDetails.EuTaxReferenceView
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

class EuTaxReferenceControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val euTaxReference: String = genEuTaxReference.sample.value

  private val formProvider = new EuTaxReferenceFormProvider()
  private val form: Form[String] = formProvider(country)

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(RegistrationTypePage(countryIndex(0)), RegistrationType.TaxId).success.value

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  val validAnswer = 0

  lazy val euTaxReferenceRoute: String = routes.EuTaxReferenceController.onPageLoad(waypoints, countryIndex(0)).url

  "EuTaxReference Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuTaxReferenceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(EuTaxReferencePage(countryIndex(0)), euTaxReference).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val view = application.injector.instanceOf[EuTaxReferenceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(euTaxReference), waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn 
          None.toFuture
        
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", euTaxReference))

        val result = route(application, request).value
        val expectedAnswers: UserAnswers = updatedAnswers
          .set(EuTaxReferencePage(countryIndex(0)), euTaxReference).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuTaxReferencePage(countryIndex(0))
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuTaxReferenceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euTaxReferenceRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
