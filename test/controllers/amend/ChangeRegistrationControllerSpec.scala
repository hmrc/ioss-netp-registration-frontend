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

package controllers.amend

import base.SpecBase
import config.Constants.maxSchemes
import models.domain.{PreviousSchemeDetails, VatCustomerInfo}
import models.previousRegistrations.{NonCompliantDetails, PreviousRegistrationDetails}
import models.{DesAddress, TradingName, UserAnswers}
import org.scalacheck.Gen
import pages.amend.ChangeRegistrationPage
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.HasTradingNamePage
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{BusinessBasedInUKPage, BusinessContactDetailsPage, ClientHasVatNumberPage, ClientVatNumberPage}
import play.api.i18n.Messages
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.SummaryListFluency
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.ChangeRegistrationView

import java.time.{Instant, LocalDate}

class ChangeRegistrationControllerSpec extends SpecBase with SummaryListFluency {

  private val iossNum = "IN012345678"
  private val amendYourAnswersPage = ChangeRegistrationPage(iossNum)

  override val vatCustomerInfo: VatCustomerInfo = {
    VatCustomerInfo(
      registrationDate = LocalDate.now(),
      desAddress = DesAddress(
        line1 = "1818 East Tusculum Street",
        line2 = Some("Phil Tow"),
        line3 = None, line4 = None, line5 = None,
        postCode = Some("BT4 2XW"),
        countryCode = "EL"),
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None
    )
  }

  val previousRegistration: PreviousRegistrationDetails = {
    PreviousRegistrationDetails(
      previousEuCountry = arbitraryCountry.arbitrary.sample.value,
      previousSchemesDetails = Gen.listOfN(
        maxSchemes, PreviousSchemeDetails(
          previousScheme = arbitraryPreviousScheme.arbitrary.sample.value,
          previousSchemeNumbers = arbitraryPreviousIossSchemeDetails.arbitrary.sample.value,
          nonCompliantDetails = Gen.option(NonCompliantDetails(
            Gen.option(Gen.choose(0, 2).sample.value).sample.value,
            Gen.option(Gen.choose(0, 2).sample.value).sample.value)
          ).sample.value
        )
      ).sample.value
    )
  }

  override def basicUserAnswersWithVatInfo: UserAnswers =
    UserAnswers(id = "12345-credId", vatInfo = Some(vatCustomerInfo), lastUpdated = Instant.now())

  private val registrationDetailsUserAnswersWithVatInfo: UserAnswers =
    basicUserAnswersWithVatInfo
      .set(BusinessBasedInUKPage, true).success.value
      .set(ClientHasVatNumberPage, true).success.value
      .set(ClientVatNumberPage, "GB123456").success.value

  private val completeUserAnswersWithVatInfo: UserAnswers =
    registrationDetailsUserAnswersWithVatInfo
      .set(HasTradingNamePage, true).success.value
      .set(AllTradingNamesQuery, List(TradingName("Some Trading Name"))).success.value
      .set(PreviouslyRegisteredPage, true).success.value
      .set(AllPreviousRegistrationsQuery, List(previousRegistration)).success.value
      .set(HasFixedEstablishmentPage, true).success.value
      .set(AllEuDetailsQuery, List(arbitraryEuDetails.arbitrary.sample.value)).success.value
      .set(BusinessContactDetailsPage, businessContactDetails).success.value


  "ChangeRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

      running(application) {

        val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(iossNumber = iossNum).url)

        implicit val msgs: Messages = messages(application)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeRegistrationView]

        val registrationList = SummaryListViewModel(rows = getRegistrationDetailsList(completeUserAnswersWithVatInfo))

        val importOneStopShopDetailsList = SummaryListViewModel(rows = getImportOneStopShopDetailsSummaryList(completeUserAnswersWithVatInfo))

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(
            waypoints,
            vatCustomerInfo.organisationName.get,
            iossNum,
            registrationList,
            importOneStopShopDetailsList
          )(request, messages(application)).toString
      }
    }
  }

  private def getRegistrationDetailsList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {

    Seq(
      BusinessBasedInUKSummary.rowWithoutAction(waypoints, answers),
      ClientHasVatNumberSummary.rowWithoutAction(waypoints, answers),
      ClientVatNumberSummary.rowWithoutAction(waypoints, answers),
      VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, answers, amendYourAnswersPage)
    ).flatten
  }

  private def getImportOneStopShopDetailsSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.changeRegRow(answers, waypoints, amendYourAnswersPage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage)
    val formattedHasTradingNameSummary = maybeHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
      if (tradingNameSummaryRow.nonEmpty) {
        nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptHasTradingNameSummaryRow
      }
    }
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(answers, waypoints)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(answers, Seq.empty, waypoints)
    val formattedPreviouslyRegisteredSummaryRowy = previouslyRegisteredSummaryRow.map { nonOptPreviouslyRegisteredSummaryRow =>
      if (previousRegistrationSummaryRow.isDefined) {
        nonOptPreviouslyRegisteredSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptPreviouslyRegisteredSummaryRow
      }
    }
    val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, amendYourAnswersPage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage)
    val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
      if (euDetailsSummaryRow.nonEmpty) {
        nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
      }
    }
    val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, answers, amendYourAnswersPage).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, amendYourAnswersPage).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, amendYourAnswersPage)

    Seq(
      formattedHasTradingNameSummary,
      tradingNameSummaryRow,
      formattedPreviouslyRegisteredSummaryRowy,
      previousRegistrationSummaryRow,
      formattedHasFixedEstablishmentSummaryRow,
      euDetailsSummaryRow,
      WebsiteSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage),
      formattedContactName,
      formattedTelephoneNumber,
      formattedEmailAddress
    ).flatten

  }

}
