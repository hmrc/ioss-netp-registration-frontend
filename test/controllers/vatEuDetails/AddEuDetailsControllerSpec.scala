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
import forms.vatEuDetails.AddEuDetailsFormProvider
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, InternationalAddress, RegistrationType, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, Waypoints}
import pages.vatEuDetails.*
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.vatEuDetails.EuDetailsSummary
import views.html.vatEuDetails.AddEuDetailsView

import scala.concurrent.Future

class AddEuDetailsControllerSpec extends SpecBase with MockitoSugar {

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

  val formProvider = new AddEuDetailsFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val addEuDetailsRoute: String = routes.AddEuDetailsController.onPageLoad(waypoints).url

  private def addEuDetailsRoutePost(waypoints: Waypoints = waypoints): String =
    routes.AddEuDetailsController.onSubmit(waypoints, incompletePromptShown = false).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), RegistrationType.VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

  "AddEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, updatedAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form, waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries has been reached" in {

      val userAnswers = (0 to Country.euCountries.size).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(EuCountryPage(countryIndex(index)), country).success.value
          .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(countryIndex(index)), RegistrationType.VatNumber).success.value
          .set(EuVatNumberPage(countryIndex(index)), euVatNumber).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, userAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, euDetailsSummaryList, canAddEuDetails = false)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, addEuDetailsRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddEuDetailsPage(), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe AddEuDetailsPage().navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, s"$addEuDetailsRoute?incompletePromptShown=true")
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, updatedAnswers, AddEuDetailsPage())

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$addEuDetailsRoute?incompletePromptShown=false")
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
