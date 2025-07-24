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
import forms.previousRegistrations.PreviousOssNumberFormProvider
import models.core.{Match, MatchType, TraderId}
import models.core.MatchType.*
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.PreviousSchemeHintText
import models.{Country, CountryWithValidationDetails, Index}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatestplus.mockito.MockitoSugar
import pages.Waypoints
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import views.html.previousRegistrations.PreviousOssNumberView
import utils.FutureSyntax.FutureOps

class PreviousOssNumberControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private val index = Index(0)
  private val country = Country("LT", "Lithuania")
  private val countryWithValidation = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == "LT").value
  private val formProvider = new PreviousOssNumberFormProvider()
  private val form = formProvider(country, Seq.empty)

  private def previousOssNumberRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(waypoints, index, index).url

  private val baseAnswers = emptyUserAnswersWithVatInfo.set(PreviousEuCountryPage(index), country).success.value

  private val mockCoreRegistartionValidationService = mock[CoreRegistrationValidationService]

  def createMatchResponse(
                         matchType: MatchType = TraderIdActiveNETP,
                         traderId: TraderId = TraderId("123456789"),
                         exclusionStatusCode: Option[Int] = None
                         ): Match = Match(
    matchType = matchType,
    traderId = traderId,
    intermediary = None,
    memberState = "DE",
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = Some("2022-10-10"),
    nonCompliantReturns = None,
    nonCompliantPayments = None
  )

  "PreviousOssNumber Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          val view = application.injector.instanceOf[PreviousOssNumberView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form, waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = baseAnswers.set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("answer")).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form.fill("answer"), waypoints, index, index, countryWithValidation, PreviousSchemeHintText.Both)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .build()

      running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[PreviousOssNumberView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, index, index, countryWithValidation,
            PreviousSchemeHintText.Both)(request, messages(application)).toString

      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
          val request = FakeRequest(GET, previousOssNumberRoute(waypoints))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
          val request =
            FakeRequest(POST, previousOssNumberRoute(waypoints))
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
            traderId = TraderId("333344445")
          )

          when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeRegistrationMatch).toFuture

          val request = FakeRequest(POST, previousOssNumberRoute(waypoints))
            .withFormUrlEncodedBody(("value", "LT333344445"))

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
        (TraderIdQuarantinedNETP, None),
        (OtherMSNETPQuarantinedNETP, None),
        (FixedEstablishmentQuarantinedNETP, None)
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
            traderId = TraderId("333344446"),
            exclusionStatusCode = exclusionStatusCode
          )

          when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
            Some(quarantinedIntermediaryMatch).toFuture

          val request = FakeRequest(POST, previousOssNumberRoute(waypoints))
            .withFormUrlEncodedBody(("value", "LT333344446"))

          val result = route(application, request).value

          status(result) `mustEqual` SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url
        }
      }
    }
  }
}
