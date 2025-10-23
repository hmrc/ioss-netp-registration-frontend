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
import models.{Country, CountryWithValidationDetails, Index, PreviousScheme}
import models.core.{Match, TraderId}
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.{NonCompliantDetails, PreviousSchemeHintText}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.Waypoints
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.previousRegistrations.NonCompliantDetailsQuery
import repositories.SessionRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.PreviousOssNumberView

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
                           traderId: TraderId = TraderId("123456789"),
                           exclusionStatusCode: Option[Int] = None,
                           nonCompliantDetails: Option[NonCompliantDetails] = None
                         ): Match = Match(
    traderId = traderId,
    intermediary = None,
    memberState = "DE",
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = Some("2022-10-10"),
    nonCompliantReturns = nonCompliantDetails.flatMap(_.nonCompliantReturns),
    nonCompliantPayments = nonCompliantDetails.flatMap(_.nonCompliantPayments)
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

    "must redirect to OtherCountryExcludedAndQuarantined for a POST if a quarantined intermediary trader is found" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn true.toFuture


      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistartionValidationService)
          )
          .build()

      running(application) {

        val quarantinedIntermediaryMatch = createMatchResponse(
          traderId = TraderId("333344446"),
          exclusionStatusCode = Some(4)
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

        val quarantinedIntermediaryMatch = createMatchResponse(
          traderId = TraderId("333344446"),
          exclusionStatusCode = Some(6),
          nonCompliantDetails = Some(nonCompliantDetails)
        )

        when(mockCoreRegistartionValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn
          Some(quarantinedIntermediaryMatch).toFuture

        val request = FakeRequest(POST, previousOssNumberRoute(waypoints))
          .withFormUrlEncodedBody(("value", "LT333344446"))

        val result = route(application, request).value

        val expectedAnswers = baseAnswers
          .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("LT333344446")).success.value
          .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
          .set(NonCompliantDetailsQuery(index, index), nonCompliantDetails).success.value

        status(result) `mustEqual` SEE_OTHER
        redirectLocation(result).value mustEqual controllers.previousRegistrations.routes.CheckPreviousSchemeAnswersController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }
  }
}
