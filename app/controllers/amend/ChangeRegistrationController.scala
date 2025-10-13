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

import controllers.GetClientCompanyName
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.summarylist.*
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.ChangeRegistrationView

import javax.inject.Inject

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: ChangeRegistrationView
                                            ) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName {

  def onPageLoad(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      val waypoints = EmptyWaypoints
      val userAnswers = request.userAnswers
      val thisPage = CheckYourAnswersPage

      getClientCompanyName(waypoints) { companyName =>
        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            BusinessBasedInUKSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientHasVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, request.userAnswers, CheckYourAnswersPage)
          ).flatten
        )


        //TradingNameSummary
        val maybeHasTradingNameSummaryRow = HasTradingNameSummary.changeRegRow(request.userAnswers, waypoints, CheckYourAnswersPage)
        val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, userAnswers, thisPage)

        val formattedHasTradingNameSummary = maybeHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
          if (tradingNameSummaryRow.nonEmpty) {
            nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            nonOptHasTradingNameSummaryRow
          }
        }


        //PreviousRegSummary
        val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(userAnswers, waypoints)
        val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(userAnswers, Seq.empty, waypoints)

        val formattedPreviouslyRegisteredSummaryRowy = previouslyRegisteredSummaryRow.map { nonOptPreviouslyRegisteredSummaryRow =>
          if (previousRegistrationSummaryRow.isDefined) {
            nonOptPreviouslyRegisteredSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            nonOptPreviouslyRegisteredSummaryRow
          }
        }


        //Fixed Establishment
        val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, userAnswers, thisPage)
        val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, userAnswers, thisPage)

        val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
          if (euDetailsSummaryRow.nonEmpty) {
            nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
          }
        }


        //Business Contact details
        val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, userAnswers, thisPage).map(_.withCssClass("govuk-summary-list__row--no-border"))
        val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, userAnswers, thisPage).map(_.withCssClass("govuk-summary-list__row--no-border"))
        val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, userAnswers, thisPage)

        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            formattedHasTradingNameSummary,
            tradingNameSummaryRow,
            formattedPreviouslyRegisteredSummaryRowy,
            previousRegistrationSummaryRow,
            formattedHasFixedEstablishmentSummaryRow,
            euDetailsSummaryRow,
            WebsiteSummary.checkAnswersRow(waypoints, userAnswers, thisPage),
            formattedContactName,
            formattedTelephoneNumber,
            formattedEmailAddress
          ).flatten
        )

        Ok(view(waypoints, companyName, iossNumber, registrationDetailsList, importOneStopShopDetailsList)).toFuture

      }

  }

  def onSubmit(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData {
    Ok(Json.toJson("done"))
  }
}
