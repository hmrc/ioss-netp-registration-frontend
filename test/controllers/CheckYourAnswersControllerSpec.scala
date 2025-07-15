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
import models.{BusinessContactDetails, CheckMode, Index, TradingName, UserAnswers, Website}
import pages.*
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.website.WebsitePage
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testutils.CheckYourAnswersSummaries.{getCYASummaryList, getCYAVatDetailsSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

  private val updatedAnswersWithVatInfo = emptyUserAnswersWithVatInfo
    .set(BusinessBasedInUKPage, true).success.value
    .set(ClientHasVatNumberPage, true).success.value
    .set(ClientVatNumberPage, vatNumber).success.value

  private val completeUserAnswers: UserAnswers = updatedAnswersWithVatInfo
    .set(HasTradingNamePage, true).success.value
    .set(TradingNamePage(Index(0)), TradingName("Test trading name")).success.value
    .set(PreviouslyRegisteredPage, false).success.value
    .set(WebsitePage(Index(0)), Website("www.test-website.com")).success.value
    .set(BusinessContactDetailsPage, businessContactDetails).success.value
    .set(HasFixedEstablishmentPage, false).success.value


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

          val missingAnswers = emptyUserAnswers
            .set(HasTradingNamePage, true).success.value

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

      "must redirect to the next page when navigating to the declaration" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` CheckYourAnswersPage.navigate(waypoints, completeUserAnswers, completeUserAnswers).url
        }
      }

      "must submit completed answers" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .build()

        running(application) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` CheckYourAnswersPage.navigate(waypoints, completeUserAnswers, completeUserAnswers).url
        }
      }

      "must redirect to the correct page when there is incomplete data" in {

        val incompleteAnswers: UserAnswers = completeUserAnswers
          .remove(WebsitePage(Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
          .build()

        running(application) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` WebsitePage(Index(0)).route(waypoints).url
        }
      }
    }
  }
}
