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

package controllers

import base.SpecBase
import connectors.RegistrationConnector
import models.responses.InternalServerError
import models.{BusinessContactDetails, CheckMode, Index, TradingName, UserAnswers, Website}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.*
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.website.WebsitePage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testutils.CheckYourAnswersSummaries.{getCYASummaryList, getCYAVatDetailsSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

  private val updatedAnswersWithVatInfo = emptyUserAnswersWithVatInfo
    .set(BusinessBasedInUKPage, true).success.value
    .set(ClientHasVatNumberPage, true).success.value
    .set(ClientVatNumberPage, vatNumber).success.value

  private val completeUserAnswers: UserAnswers = updatedAnswersWithVatInfo
    .set(HasTradingNamePage, true).success.value
    .set(TradingNamePage(Index(0)), TradingName("Test trading name")).success.value
    .set(WebsitePage(Index(0)), Website("www.test-website.com")).success.value
    .set(BusinessContactDetailsPage, businessContactDetails).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  "Check Your Answers Controller" - {

    ".onPageLoad" - {

      val waypoints: NonEmptyWaypoints = EmptyWaypoints
        .setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

      "must return OK and the correct view for a GET" - {

        "with completed data present" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val vatDetailsList: SummaryList = SummaryListViewModel(getCYAVatDetailsSummaryList(waypoints, updatedAnswersWithVatInfo, CheckYourAnswersPage))
            val list = SummaryListViewModel(getCYASummaryList(waypoints, completeUserAnswers, CheckYourAnswersPage))

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = true)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {

          val missingAnswers: UserAnswers = completeUserAnswers
            .remove(BusinessBasedInUKPage).success.value
            .remove(ClientHasVatNumberPage).success.value
            .remove(ClientVatNumberPage).success.value
            .remove(WebsitePage(Index(0))).success.value
            .remove(BusinessContactDetailsPage).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers)).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val vatDetailsList: SummaryList = SummaryListViewModel(getCYAVatDetailsSummaryList(waypoints, missingAnswers, CheckYourAnswersPage))
            val list = SummaryListViewModel(getCYASummaryList(waypoints, missingAnswers, CheckYourAnswersPage))

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = false)(request, messages(application)).toString
          }
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the next page for a POST when a valid response is received from the backend" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Right(()).toFuture

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` CheckYourAnswersPage.navigate(waypoints, completeUserAnswers, completeUserAnswers).url
          verify(mockRegistrationConnector, times(1)).submitPendingRegistration(eqTo(completeUserAnswers))(any())
        }
      }

      "must redirect to the correct page when an error is returned from the backend" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ErrorSubmittingPendingRegistrationPage.route(waypoints).url
          verify(mockRegistrationConnector, times(1)).submitPendingRegistration(eqTo(completeUserAnswers))(any())
        }
      }

      "must redirect to the correct page when there is incomplete data" in {


        val incompleteAnswers: UserAnswers = completeUserAnswers
          .remove(WebsitePage(Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.submitPendingRegistration(any())(any())) thenReturn Right(()).toFuture

        running(application) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` WebsitePage(Index(0)).route(waypoints).url
          verify(mockRegistrationConnector, times(1)).submitPendingRegistration(eqTo(incompleteAnswers))(any())
        }
      }
    }
  }
}
