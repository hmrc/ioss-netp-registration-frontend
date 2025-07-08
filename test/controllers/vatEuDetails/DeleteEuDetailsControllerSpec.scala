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
import forms.vatEuDetails.DeleteEuDetailsFormProvider
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, InternationalAddress, RegistrationType, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.{AllEuDetailsRawQuery, EuDetailsQuery}
import repositories.{AuthenticatedUserAnswersRepository, SessionRepository}
import views.html.vatEuDetails.DeleteEuDetailsView
import utils.FutureSyntax.FutureOps


class DeleteEuDetailsControllerSpec extends SpecBase with MockitoSugar {

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

  val formProvider = new DeleteEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider(country)

  lazy val deleteEuDetailsRoute: String = routes.DeleteEuDetailsController.onPageLoad(waypoints, countryIndex(0)).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), RegistrationType.VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

  "DeleteEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteEuDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must remove the record and redirect to the next page when the user answers Yes" in {

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
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .remove(EuDetailsQuery(countryIndex(0))).success.value
          .remove(AllEuDetailsRawQuery).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustEqual DeleteEuDetailsPage(countryIndex(0)).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not remove the record and then redirect to the next page when the user answers No" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteEuDetailsPage(countryIndex(0)).navigate(waypoints, updatedAnswers, updatedAnswers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteEuDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, countryIndex(0), country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteEuDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }

  "must redirect to Journey Recovery for a POST if the EU Registration is not found" in {

    val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

    running(application) {
      val request =
        FakeRequest(POST, deleteEuDetailsRoute)
          .withFormUrlEncodedBody(("value", "true"))

      val result = route(application, request).value

      status(result) `mustBe` SEE_OTHER
      redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
    }
  }
}
