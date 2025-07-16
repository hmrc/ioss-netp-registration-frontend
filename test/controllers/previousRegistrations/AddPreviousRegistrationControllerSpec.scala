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
import controllers.previousRegistrations.routes as prevRoutes
import controllers.routes
import forms.previousRegistrations.AddPreviousRegistrationFormProvider
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.{PreviousRegistrationDetailsWithOptionalVatNumber, SchemeDetailsWithOptionalVatNumber}
import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.previousRegistrations.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import viewmodels.previousRegistrations.PreviousRegistrationSummary
import views.html.previousRegistrations.AddPreviousRegistrationView

import scala.concurrent.Future

class AddPreviousRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new AddPreviousRegistrationFormProvider()
  private val form = formProvider()

  private lazy val addPreviousRegistrationRoute = prevRoutes.AddPreviousRegistrationController.onPageLoad(EmptyWaypoints).url

  private def addPreviousRegistrationRoutePost(prompt: Boolean, waypoints: Waypoints = EmptyWaypoints): String =
    prevRoutes.AddPreviousRegistrationController.onSubmit(waypoints, prompt).url

  private val baseAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
      .set(PreviousSchemePage(Index(0), Index(0)), PreviousScheme.OSSU).success.value
      .set(PreviousOssNumberPage(Index(0), Index(0)), PreviousSchemeNumbers("foo")).success.value
      .set(ClientHasIntermediaryPage(Index(0), Index(0)), false).success.value

  private val incompleteAnswers =
    basicUserAnswersWithVatInfo
      .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
      .set(PreviousSchemePage(Index(0), Index(0)), PreviousScheme.OSSU).success.value

  "AddPreviousRegistration Controller" - {

    "must return OK and the correct view for a GET when answers are complete" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addPreviousRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddPreviousRegistrationView]
        implicit val msgs: Messages = messages(application)
        val list = PreviousRegistrationSummary.row(
          answers = baseAnswers,
          existingPreviousRegistrations = Seq.empty,
          waypoints = EmptyWaypoints,
          sourcePage = AddPreviousRegistrationPage()
        )

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, EmptyWaypoints, list, canAddCountries = true)(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET when answers aren't complete" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addPreviousRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddPreviousRegistrationView]
        implicit val msgs: Messages = messages(application)
        val list = PreviousRegistrationSummary.row(
          answers = incompleteAnswers,
          existingPreviousRegistrations = Seq.empty,
          waypoints = EmptyWaypoints,
          sourcePage = AddPreviousRegistrationPage()
        )

        status(result) `mustBe` OK
        contentAsString(result) `mustBe`
          view(
            form,
            EmptyWaypoints,
            list,
            canAddCountries = true,
            Seq(
              PreviousRegistrationDetailsWithOptionalVatNumber(
                Country.euCountries.head,
                Some(List(SchemeDetailsWithOptionalVatNumber(Some(PreviousScheme.OSSU), clientHasIntermediary = Some(false), None)))
              )
            )
          )(request, implicitly).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(false))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(AddPreviousRegistrationPage(), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddPreviousRegistrationPage().navigate(EmptyWaypoints, baseAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(false))
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddPreviousRegistrationView]
        implicit val msgs: Messages = messages(application)
        val list = PreviousRegistrationSummary.row(baseAnswers, Seq.empty, waypoints, AddPreviousRegistrationPage())
        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, list, canAddCountries = true)(request, implicitly).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addPreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, addPreviousRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(false))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must refresh the page for a POST if answers are incomplete and prompt has not been shown" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(false))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` prevRoutes.AddPreviousRegistrationController.onPageLoad(EmptyWaypoints).url
      }
    }

    "must redirect to the PreviousEuVatNumber page for a POST if answers are incomplete and prompt has been shown" in {

      val incompleteAnswersWithPreviousSchemeType = incompleteAnswers
        .set(PreviousSchemeTypePage(Index(0), Index(0)), PreviousSchemeType.OSS).success.value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswersWithPreviousSchemeType)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` prevRoutes.PreviousOssNumberController.onPageLoad(EmptyWaypoints, Index(0), Index(0)).url
      }
    }

    "must redirect to the PreviousIOSSNumber page for a POST if answers are incomplete and prompt has been shown and previousScheme is set" in {

      val incompleteAnswersWithPreviousSchemeType = basicUserAnswersWithVatInfo
        .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
        .set(PreviousSchemeTypePage(Index(0), Index(0)), PreviousSchemeType.IOSS).success.value
        .set(PreviousSchemePage(Index(0), Index(0)), PreviousScheme.IOSSWI).success.value
        .set(ClientHasIntermediaryPage(Index(0), Index(0)), true).success.value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswersWithPreviousSchemeType)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` prevRoutes.PreviousIossNumberController.onPageLoad(EmptyWaypoints, Index(0), Index(0)).url
      }
    }

    "must redirect to the PreviousIOSSNumber page for a POST if answers are incomplete and prompt has been shown and previousScheme is not set" in {

      val incompleteAnswersWithPreviousSchemeType = basicUserAnswersWithVatInfo
        .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
        .set(PreviousSchemeTypePage(Index(0), Index(0)), PreviousSchemeType.IOSS).success.value
        .set(ClientHasIntermediaryPage(Index(0), Index(0)), true).success.value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswersWithPreviousSchemeType)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` prevRoutes.PreviousIossNumberController.onPageLoad(EmptyWaypoints, Index(0), Index(0)).url
      }
    }

    "must redirect to the PreviousScheme page for a POST if answers are incomplete and prompt has been shown and previousSchemeDetails is not set" in {

      val incompleteAnswersWithPreviousSchemeType = basicUserAnswersWithVatInfo
        .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswersWithPreviousSchemeType)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` prevRoutes.PreviousSchemeController.onPageLoad(EmptyWaypoints, Index(0), Index(0)).url
      }
    }

    "must redirect to the ClientHasIntermediary page for a POST if the answer is incomplete and prompt has been shown" in {

      val incompleteAnswersWithPreviousSchemeType = basicUserAnswersWithVatInfo
        .set(PreviousEuCountryPage(Index(0)), Country.euCountries.head).success.value
        .set(PreviousSchemeTypePage(Index(0), Index(0)), PreviousSchemeType.IOSS).success.value
        .set(PreviousIossNumberPage(Index(0), Index(0)), PreviousSchemeNumbers("123456789")).success.value
        .set(PreviousSchemePage(Index(0), Index(0)), PreviousScheme.IOSSWI).success.value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswersWithPreviousSchemeType)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousRegistrationRoutePost(true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` ClientHasIntermediaryPage(Index(0), Index(0)).route(waypoints).url
      }
    }
  }
}
