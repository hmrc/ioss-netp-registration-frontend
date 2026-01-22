/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.routes as normalRoutes
import forms.vatEuDetails.EuVatNumberFormProvider
import models.core.TraderId
import models.vatEuDetails.RegistrationType.VatNumber
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, CountryWithValidationDetails, Index, InternationalAddress, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.*
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import testutils.CreateMatchResponse.createMatchResponse
import utils.FutureSyntax.FutureOps
import views.html.vatEuDetails.EuVatNumberView

import java.time.{Clock, Instant, ZoneId}

class EuVatNumberControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val countryWithValidation: CountryWithValidationDetails = CountryWithValidationDetails.euCountriesWithVRNValidationRules
    .find(_.country.code == country.code).value
  private val tradingName: TradingName = arbitraryTradingName.arbitrary.sample.value
  private val businessAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val formProvider: EuVatNumberFormProvider = new EuVatNumberFormProvider()
  private val form: Form[String] = formProvider(country)

  private lazy val euVatNumberRoute: String = routes.EuVatNumberController.onPageLoad(waypoints, countryIndex(0)).url

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(Index(0)),
      TradingNameAndBusinessAddress(tradingName, businessAddress)
    ).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "EuVatNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuVatNumberView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex(0), countryWithValidation)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(EuVatNumberPage(countryIndex(0)), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val view = application.injector.instanceOf[EuVatNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill("answer"), waypoints, countryIndex(0), countryWithValidation)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn
          None.toFuture

        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` EuVatNumberPage(countryIndex(0)).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not save the answers and redirect to Client Already Registered page when a non-intermediary user is already registered on another service with exclusion pending" in {

      val today: Instant = Instant.now(stubClockAtArbitraryDate)
      val todayClock: Clock = Clock.fixed(today, ZoneId.systemDefault())

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(
          userAnswers = Some(updatedAnswers),
          clock = Some(todayClock)
        )
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val activeRegistrationMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionEffectiveDate = Some(today.atZone(ZoneId.systemDefault()).toLocalDate.plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.ClientAlreadyRegisteredController.onPageLoad(activeRegistrationMatch.getEffectiveDate).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(countryCode))(any(), any())
      }
    }

    "must not save the answers and redirect to Other Country Excluded And Quarantined page when a quarantined intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val quarantinedIntermediaryMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionStatusCode = Some(4)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(quarantinedIntermediaryMatch).toFuture

        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` normalRoutes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          quarantinedIntermediaryMatch.memberState,
          quarantinedIntermediaryMatch.getEffectiveDate
        ).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(countryCode))(any(), any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuVatNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, countryIndex(0), countryWithValidation)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euVatNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application: Application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, euVatNumberRoute)
            .withFormUrlEncodedBody(("value", euVatNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}