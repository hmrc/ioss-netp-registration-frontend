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
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.{Country, Index, InternationalAddress, RegistrationType, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.vatEuDetails.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.vatEuDetails.*
import viewmodels.govuk.SummaryListFluency
import views.html.vatEuDetails.CheckEuDetailsAnswersView
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

class CheckEuDetailsAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val countryIndex: Index = Index(0)
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
  lazy val checkEuDetailsAnswersRoute: String = routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex(0)).url

  private def checkEuDetailsAnswersSubmitRoute() =
    routes.CheckEuDetailsAnswersController.onSubmit(waypoints, countryIndex(0), incompletePromptShown = false).url

  private val checkEuDetailsAnswersPage: CheckEuDetailsAnswersPage = CheckEuDetailsAnswersPage(countryIndex(0))

  private val answers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), RegistrationType.VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

  "CheckEuDetailsAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckEuDetailsAnswersView]

        val summaryList: SummaryList = SummaryListViewModel(
          rows = Seq(
            EuCountrySummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            TradingNameAndBusinessAddressSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            RegistrationTypeSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            EuVatNumberSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            EuTaxReferenceSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
          ).flatten
        )

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(waypoints, countryIndex(0), country, summaryList)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute())

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe CheckEuDetailsAnswersPage(countryIndex(0)).navigate(waypoints, answers, answers).url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to the next page when answers are complete on a POST" in {

      val mockSessionRepository = mock[SessionRepository]

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      running(application) {
        val request = FakeRequest(POST, routes.CheckEuDetailsAnswersController.onSubmit(waypoints, countryIndex, incompletePromptShown = false).url)
        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, answers, answers).url
      }
    }
  }
}
