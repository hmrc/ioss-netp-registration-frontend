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

package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.PreviousIossNumberFormProvider
import models.{Country, Index, PreviousScheme}
import models.core.{Match, MatchType, TraderId}
import models.core.MatchType.*
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.NonCompliantDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatestplus.mockito.MockitoSugar
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.previousRegistrations.NonCompliantDetailsQuery
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousIossNumberView

class PreviousIossNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val index = Index(0)

  private val country = Country.euCountries.head
  private val formProvider = new PreviousIossNumberFormProvider()
  private val form = formProvider(country)

  private lazy val previousIossNumberRoute = controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(waypoints, index, index).url

  private val iossHintText = "This will start with IM040 followed by 7 numbers"

  private val baseAnswers = emptyUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  private val mockCoreRegistartionValidationService = mock[CoreRegistrationValidationService]

  def createMatchResponse(
                           matchType: MatchType = TraderIdActiveNETP,
                           traderId: TraderId = TraderId("IM0987654321"),
                           exclusionStatusCode: Option[Int] = None,
                           nonCompliantDetails: Option[NonCompliantDetails] = None
                         ): Match = Match(
    matchType = matchType,
    traderId = traderId,
    intermediary = None,
    memberState = "DE",
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = Some("2022-10-10"),
    nonCompliantReturns = nonCompliantDetails.flatMap(_.nonCompliantReturns),
    nonCompliantPayments = nonCompliantDetails.flatMap(_.nonCompliantPayments)
  )

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

    "must Redirect to ClientAlreadyRegistered for a post if an active non-intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val testConditions = Table(
        ("MatchType", "exclusionStatusCode"),
        (TraderIdActiveNETP, None),
        (OtherMSNETPActiveNETP, None),
        (FixedEstablishmentActiveNETP, None)
      )

      forAll(testConditions) { (matchType, exclusionStatusCode) =>

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistartionValidationService)
            )
            .build()

        running(application) {

          val activeRegistrationMatch = createMatchResponse(
            matchType = matchType,
            traderId = TraderId("IM0987654321")
          )

          when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

          val request = FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", "IM0401234567"))

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url
        }
      }
    }

    "must redirect to OtherCountryExcludedAndQuarantined for a POST if a quarantined intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val testConditions = Table(
        ("MatchType", "exclusionStatusCode"),
        (TraderIdQuarantinedNETP, Some(4)),
        (OtherMSNETPQuarantinedNETP, Some(4)),
        (FixedEstablishmentQuarantinedNETP, Some(4))
      )

      forAll(testConditions) { (matchType, exclusionStatusCode) =>

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistartionValidationService)
            )
            .build()

        running(application) {

          val quarantinedIntermediaryMatch = createMatchResponse(
            matchType = matchType,
            traderId = TraderId("IM0987654321"),
            exclusionStatusCode = exclusionStatusCode
          )

          when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
            Some(quarantinedIntermediaryMatch).toFuture

          val request = FakeRequest(POST, previousIossNumberRoute)
            .withFormUrlEncodedBody(("value", "IM0401234567"))

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url
        }
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
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistartionValidationService)
          )
          .build()

      running(application) {

        val activeMatch = createMatchResponse(
          matchType = MatchType.TransferringMSID,
          traderId = TraderId("IM0987654321"),
          exclusionStatusCode = Some(6),
          nonCompliantDetails = Some(nonCompliantDetails)
        )

        when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Some(activeMatch).toFuture

        val request = FakeRequest(POST, previousIossNumberRoute)
          .withFormUrlEncodedBody(("value", "IM0401234567"))

        val result = route(application, request).value

        val expectedAnswers = baseAnswers
          .set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IM0401234567")).success.value
          .set(PreviousSchemePage(index, index), PreviousScheme.IOSSWOI).success.value
          .set(NonCompliantDetailsQuery(index, index), nonCompliantDetails).success.value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }
  }
}
