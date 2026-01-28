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

package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.PreviousIossNumberFormProvider
import models.PreviousScheme.IOSSWOI
import models.core.TraderId
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.NonCompliantDetails
import models.{ActiveTraderResult, Country, Index, PreviousScheme, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage, PreviousSchemePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ActiveTraderResultQuery
import queries.previousRegistrations.NonCompliantDetailsQuery
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import testutils.CreateMatchResponse.createMatchResponse
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousIossNumberView

import java.time.{Clock, Instant, LocalDate, ZoneId}

class PreviousIossNumberControllerSpec
  extends SpecBase
    with MockitoSugar
    with TableDrivenPropertyChecks
    with BeforeAndAfterEach {

  private val index: Index = Index(0)

  private val country: Country = Country.euCountries.head
  private val formProvider: PreviousIossNumberFormProvider = new PreviousIossNumberFormProvider()
  private val form: Form[String] = formProvider(country)

  private lazy val previousIossNumberRoute: String = controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(waypoints, index, index).url

  private val iossHintText: String = "This will start with IM040 followed by 7 numbers"

  private val baseAnswers: UserAnswers = emptyUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "PreviousIossNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousIossNumberView]

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form, waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(form.fill("answer"), waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviousIossNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, index, index, country, iossHintText)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIossNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must not save the answer but save the active match result and redirect to Client Already Registered page when an active non-intermediary trader is found with exclusion pending" in {

      val previousIossNumber: String = "IM0401234567"

      val today: Instant = Instant.now(stubClockAtArbitraryDate)
      val todayClock: Clock = Clock.fixed(today, ZoneId.systemDefault())
      val exclusionEffectiveDate: Option[String] = Some(today.atZone(ZoneId.systemDefault()).toLocalDate.plusDays(1).toString)

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
        isReversal = false,
        exclusionEffectiveDate = exclusionEffectiveDate
      )

      val answersWithActiveTraderResult: UserAnswers = emptyUserAnswers
        .set(PreviousEuCountryPage(index), country).success.value
        .set(ActiveTraderResultQuery, activeTraderResult).success.value

      val application =
        applicationBuilder(
          userAnswers = Some(answersWithActiveTraderResult),
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
          exclusionEffectiveDate = exclusionEffectiveDate
        )

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", previousIossNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(eqTo(answersWithActiveTraderResult))
        verify(mockCoreRegistrationValidationService, times(1))
          .searchScheme(eqTo(previousIossNumber), eqTo(IOSSWOI), eqTo(None), eqTo(country.code))(any(), any())
      }
    }

    "must not save the answer but save the active match result and redirect to Client Already Registered page when an active non-intermediary trader is found with no exclusion pending" in {

      val previousIossNumber: String = "IM0401234567"

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
        isReversal = false,
        exclusionEffectiveDate = None
      )

      val answersWithActiveTraderResult: UserAnswers = emptyUserAnswers
        .set(PreviousEuCountryPage(index), country).success.value
        .set(ActiveTraderResultQuery, activeTraderResult).success.value

      val application =
        applicationBuilder(userAnswers = Some(answersWithActiveTraderResult))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val activeRegistrationMatch = createMatchResponse(
          traderId = TraderId("IM0987654321")
        )

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", previousIossNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(eqTo(answersWithActiveTraderResult))
        verify(mockCoreRegistrationValidationService, times(1))
          .searchScheme(eqTo(previousIossNumber), eqTo(IOSSWOI), eqTo(None), eqTo(country.code))(any(), any())
      }
    }

    "must not save the answer but save the active match result and redirect to Client Already Registered page when an active non-intermediary trader is found with reversal pending" in {

      val previousIossNumber: String = "IM0401234567"

      val today: Instant = Instant.now(stubClockAtArbitraryDate)
      val todayClock: Clock = Clock.fixed(today, ZoneId.systemDefault())
      val exclusionEffectiveDate: Option[String] = Some(today.atZone(ZoneId.systemDefault()).toLocalDate.plusDays(1).toString)

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
        isReversal = true,
        exclusionEffectiveDate = exclusionEffectiveDate
      )

      val answersWithActiveTraderResult: UserAnswers = emptyUserAnswers
        .set(PreviousEuCountryPage(index), country).success.value
        .set(ActiveTraderResultQuery, activeTraderResult).success.value

      val application =
        applicationBuilder(
          userAnswers = Some(answersWithActiveTraderResult),
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
          exclusionEffectiveDate = exclusionEffectiveDate,
          exclusionStatusCode = Some(-1)
        )

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", previousIossNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(eqTo(answersWithActiveTraderResult))
        verify(mockCoreRegistrationValidationService, times(1))
          .searchScheme(eqTo(previousIossNumber), eqTo(IOSSWOI), eqTo(None), eqTo(country.code))(any(), any())
      }
    }

    "must not save the answers and redirect to Other Country Excluded And Quarantined page when a quarantined intermediary trader is found" in {

      val previousIossNumber: String = "IM0401234567"

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val quarantinedIntermediaryMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionStatusCode = Some(4),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Some(quarantinedIntermediaryMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", previousIossNumber))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url
        verifyNoInteractions(mockSessionRepository)
        verify(mockCoreRegistrationValidationService, times(1))
          .searchScheme(eqTo(previousIossNumber), eqTo(IOSSWOI), eqTo(None), eqTo(country.code))(any(), any())
      }
    }

    "must save and store nonCompliantDetails for a POST if active match for intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val nonCompliantDetails = NonCompliantDetails(Some(1), Some(2))

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {

        val activeMatch = createMatchResponse(
          traderId = TraderId("IM0987654321"),
          exclusionStatusCode = Some(6),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).toString),
          nonCompliantDetails = Some(nonCompliantDetails)
        )

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Some(activeMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", "IM0401234567"))

        val result = route(application, request).value

        val expectedAnswers = baseAnswers
          .set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567")).success.value
          .set(PreviousSchemePage(index, index), PreviousScheme.IOSSWOI).success.value
          .set(NonCompliantDetailsQuery(index, index), nonCompliantDetails).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }
  }
}
