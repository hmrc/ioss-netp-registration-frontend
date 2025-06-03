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
import forms.ClientBusinessAddressFormProvider
import models.{ClientBusinessName, Country, InternationalAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.ClientBusinessAddressView

import scala.concurrent.Future

class ClientBusinessAddressControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val countries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(countries).sample.value
  private val companyName: String = "Test company Name"
  private val taxReference: String = "123456789"

  val formProvider = new ClientBusinessAddressFormProvider()
  val form: Form[InternationalAddress] = formProvider(Some(country))
  private val businessAddress: InternationalAddress = InternationalAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    stateOrRegion = None,
    postCode = None,
    country = Some(country)
  )

  private val updatedAnswers = emptyUserAnswers
    .set(BusinessBasedInUKPage, false).success.value
    .set(ClientCountryBasedPage, country).success.value
    .set(ClientBusinessNamePage, ClientBusinessName(companyName)).success.value
    .set(ClientTaxReferencePage, taxReference).success.value

  lazy val clientBusinessAddressRoute: String = routes.ClientBusinessAddressController.onPageLoad(waypoints).url

  "ClientBusinessAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientBusinessAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientBusinessAddressView]

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form, waypoints, Some(country))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(ClientBusinessAddressPage, businessAddress).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientBusinessAddressRoute)

        val view = application.injector.instanceOf[ClientBusinessAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form.fill(businessAddress), waypoints, Some(country))(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clientBusinessAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", businessAddress.line1), ("townOrCity", businessAddress.townOrCity), ("country", businessAddress.country.get.name)
            )

        val result = route(application, request).value

        val expectedAnswers = emptyUserAnswers.set(ClientBusinessAddressPage, businessAddress).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ClientBusinessAddressPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clientBusinessAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ClientBusinessAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, Some(country))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, clientBusinessAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, clientBusinessAddressRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
