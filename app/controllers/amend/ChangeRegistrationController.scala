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
import models.UserAnswers
import pages.{EmptyWaypoints, Waypoints}
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
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

  def onPageLoad(waypoints: Waypoints = EmptyWaypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      val userAnswers = request.userAnswers
      val thisPage = ChangeRegistrationPage(iossNumber)

      getClientCompanyName(waypoints) { companyName =>
        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            BusinessBasedInUKSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientHasVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, request.userAnswers, thisPage)
          ).flatten
        )

        val(hasTradingNameRow, tradingNameRow) = getTradingNameRows(request.userAnswers, waypoints, thisPage)

        val(previouslyRegisteredRow, previousRegSummaryRow) = getPreviousRegRows(request.userAnswers, waypoints)

        val(hasFixedEstablishmentRow, euDetailsSummaryRow) = getFixedEstablishmentRows(waypoints, request.userAnswers, thisPage)

        val(contactNameRow, telephoneNumRow, emailRow) = getBusinessContactRows(waypoints, userAnswers, thisPage)

        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            hasTradingNameRow,
            tradingNameRow,
            previouslyRegisteredRow,
            previousRegSummaryRow,
            hasFixedEstablishmentRow,
            euDetailsSummaryRow,
            WebsiteSummary.checkAnswersRow(waypoints, userAnswers, thisPage),
            contactNameRow,
            telephoneNumRow,
            emailRow
          ).flatten
        )

        Ok(view(waypoints, companyName, iossNumber, registrationDetailsList, importOneStopShopDetailsList)).toFuture

      }

  }

  def onSubmit(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData() {
    Ok(Json.toJson(iossNumber)) //TODO VEI-199 - implement submit amend reg.
  }

  private def getTradingNameRows(answers: UserAnswers, waypoints: Waypoints, changePage: ChangeRegistrationPage)(implicit messages: Messages) = {
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(answers, waypoints, changePage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, changePage)

    val formattedHasTradingNameSummary = maybeHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
      if (tradingNameSummaryRow.nonEmpty) {
        nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptHasTradingNameSummaryRow
      }
    }
    (formattedHasTradingNameSummary, tradingNameSummaryRow)
  }

  private def getPreviousRegRows(answers: UserAnswers, waypoints: Waypoints)(implicit messages: Messages) = {
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(answers, waypoints)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(answers, Seq.empty, waypoints)

    val formattedPreviouslyRegisteredSummaryRow = previouslyRegisteredSummaryRow.map { nonOptPreviouslyRegisteredSummaryRow =>
      if (previousRegistrationSummaryRow.isDefined) {
        nonOptPreviouslyRegisteredSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptPreviouslyRegisteredSummaryRow
      }
    }

    (formattedPreviouslyRegisteredSummaryRow, previousRegistrationSummaryRow)
  }

  private def getFixedEstablishmentRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage)(implicit messages: Messages) = {
    val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, page)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, page)

    val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
      if (euDetailsSummaryRow.nonEmpty) {
        nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
      }
    }
    (formattedHasFixedEstablishmentSummaryRow, euDetailsSummaryRow)
  }

  private def getBusinessContactRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage)(implicit messages: Messages) = {

    val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, page)

    (formattedContactName, formattedTelephoneNumber, formattedEmailAddress)
  }
}
