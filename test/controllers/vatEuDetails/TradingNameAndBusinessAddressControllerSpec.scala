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
import forms.vatEuDetails.TradingNameAndBusinessAddressFormProvider
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, InternationalAddress, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.{EuCountryPage, HasFixedEstablishmentPage, TradingNameAndBusinessAddressPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.vatEuDetails.TradingNameAndBusinessAddressView
import utils.FutureSyntax.FutureOps


class TradingNameAndBusinessAddressControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress = TradingNameAndBusinessAddress (
    tradingName = TradingName("Company name"),
    address = InternationalAddress(
      line1 = "line-1",
      line2 = None,
      townOrCity = "town-or-city",
      stateOrRegion = None,
      postCode = None,
      country = Some(country)
        )
  )

  private val formProvider = new TradingNameAndBusinessAddressFormProvider()
  private val form: Form[TradingNameAndBusinessAddress] = formProvider(country)

  private lazy val TradingNameAndBusinessAddressRoute: String =
    routes.TradingNameAndBusinessAddressController.onPageLoad(waypoints, countryIndex(0)).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value

  "TradingNameAndBusinessAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, TradingNameAndBusinessAddressRoute)

        val view = application.injector.instanceOf[TradingNameAndBusinessAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(
        TradingNameAndBusinessAddressPage(countryIndex(0)),
        tradingNameAndBusinessAddress).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, TradingNameAndBusinessAddressRoute)

        val view = application.injector.instanceOf[TradingNameAndBusinessAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(tradingNameAndBusinessAddress), waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, TradingNameAndBusinessAddressRoute)
            .withFormUrlEncodedBody(
              ("tradingName", tradingNameAndBusinessAddress.tradingName.name),
              ("line1", tradingNameAndBusinessAddress.address.line1),
              ("townOrCity", tradingNameAndBusinessAddress.address.townOrCity),
              ("country",tradingNameAndBusinessAddress.address.country.toString),
            )

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` TradingNameAndBusinessAddressPage(countryIndex(0))
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, TradingNameAndBusinessAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TradingNameAndBusinessAddressView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(
          boundForm, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, TradingNameAndBusinessAddressRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, TradingNameAndBusinessAddressRoute)
            .withFormUrlEncodedBody(("line1", "value 1"), ("line2", "value 2"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
