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

package controllers

import base.SpecBase
import forms.ClientTaxReferenceFormProvider
import models.{ClientBusinessName, Country}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessBasedInUKPage, ClientBusinessNamePage, ClientCountryBasedPage, ClientTaxReferencePage, EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import views.html.ClientTaxReferenceView
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

class ClientTaxReferenceControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(countries).sample.value
  private val taxReference: String = "123456789"
  private val companyName: String = "Test company Name"
  private val updatedAnswers = emptyUserAnswers
    .set(BusinessBasedInUKPage, false).success.value
    .set(ClientCountryBasedPage, country).success.value
    .set(ClientBusinessNamePage, ClientBusinessName(companyName)).success.value

  val formProvider = new ClientTaxReferenceFormProvider()
  val form: Form[String] = formProvider(country)

  lazy val clientTaxRefrenceRoute: String = routes.ClientTaxReferenceController.onPageLoad(waypoints).url

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  "ClientTaxRefrence Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientTaxRefrenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientTaxReferenceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(ClientTaxReferencePage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientTaxRefrenceRoute)

        val view = application.injector.instanceOf[ClientTaxReferenceView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), waypoints, country)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

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

        when(mockCoreRegistrationValidationService.searchTraderId(any[String])(any(), any())) thenReturn
          None.toFuture
        
        val request =
          FakeRequest(POST, clientTaxRefrenceRoute)
            .withFormUrlEncodedBody(("value", taxReference))

        val result = route(application, request).value

        val expectedAnswers = updatedAnswers.set(ClientTaxReferencePage, taxReference).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ClientTaxReferencePage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientTaxRefrenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClientTaxReferenceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientTaxRefrenceRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientTaxRefrenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
