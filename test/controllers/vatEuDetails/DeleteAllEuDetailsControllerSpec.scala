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
import forms.vatEuDetails.DeleteAllEuDetailsFormProvider
import models.vatEuDetails.{RegistrationType, TradingNameAndBusinessAddress}
import models.{Country, InternationalAddress, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import repositories.SessionRepository
import views.html.vatEuDetails.DeleteAllEuDetailsView
import utils.FutureSyntax.FutureOps


class DeleteAllEuDetailsControllerSpec extends SpecBase with MockitoSugar {
  
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress = TradingNameAndBusinessAddress(
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
  
  val formProvider = new DeleteAllEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val deleteAllEuDetailsRoute: String = routes.DeleteAllEuDetailsController.onPageLoad(waypoints).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), RegistrationType.VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value
  

  "DeleteAllEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must remove all EU Details and then redirect to the next page when the user answers Yes" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(updatedAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, deleteAllEuDetailsRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllEuDetailsPage, true).success.value
          .remove(AllEuDetailsQuery).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllEuDetailsPage.navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not remove all EU Details and then redirect to the next page when the user answers No" in {

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
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllEuDetailsPage, false).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllEuDetailsPage.navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        val result = route(application, request).value

        status(result) `mustEqual` BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
