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
import models.domain.PreviousRegistration
import models.etmp.EtmpPreviousEuRegistrationDetails
import models.{CheckMode, UserAnswers}
import pages.*
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.*
import viewmodels.checkAnswers.vatEuDetails.*
import viewmodels.govuk.summarylist.*
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.ChangeRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              cc: AuthenticatedControllerComponents,
                                              view: ChangeRegistrationView,
                                              registrationService: RegistrationService
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndRequireRegistration(inAmend = true).async {
    implicit request =>

      val waypoints = EmptyWaypoints
        .setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

      val thisPage = ChangeRegistrationPage

      val clientBasedInUk = request.userAnswers.get(BusinessBasedInUKPage).getOrElse(false)

      val existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails] = request.registrationWrapper
        .etmpDisplayRegistration.schemeDetails.previousEURegistrationDetails

      getClientCompanyName(waypoints) { companyName =>

        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            BusinessBasedInUKSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientHasVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            if (!clientBasedInUk) ClientCountryBasedSummary.row(waypoints, request.userAnswers, thisPage) else None,
            ClientTaxReferenceSummary.row(waypoints, request.userAnswers, thisPage),
            if (request.userAnswers.vatInfo.isDefined) {
              VatRegistrationDetailsSummary.changeRegVatBusinessNameRow(waypoints, request.userAnswers, thisPage, clientBasedInUk)
            } else {
              ClientBusinessNameSummary.row(waypoints, request.userAnswers, thisPage)
            },
            ClientHasUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientsNinoNumberSummary.row(waypoints, request.userAnswers, thisPage),
            VatRegistrationDetailsSummary.changeRegBusinessAddressRow(waypoints, request.userAnswers, thisPage),
            if (clientBasedInUk) ClientBusinessAddressSummary.changeUkBasedRegRow(waypoints, request.userAnswers, thisPage) else ClientBusinessAddressSummary.row(waypoints, request.userAnswers, thisPage)
          ).flatten
        )

        val (hasTradingNameRow, tradingNameRow) = getTradingNameRows(request.userAnswers, waypoints, thisPage)

        val (previouslyRegisteredRow, previousRegSummaryRow) = getPreviousRegRows(request.userAnswers, waypoints, thisPage, existingPreviousRegistrations)

        val (hasFixedEstablishmentRow, euDetailsSummaryRow) = getFixedEstablishmentRows(waypoints, request.userAnswers, thisPage)

        val (contactNameRow, telephoneNumRow, emailRow) = getBusinessContactRows(waypoints, request.userAnswers, thisPage)

        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            hasTradingNameRow,
            tradingNameRow,
            previouslyRegisteredRow,
            previousRegSummaryRow,
            hasFixedEstablishmentRow,
            euDetailsSummaryRow,
            WebsiteSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage),
            contactNameRow,
            telephoneNumRow,
            emailRow
          ).flatten
        )

        Ok(view(waypoints, companyName, request.iossNumber, registrationDetailsList, importOneStopShopDetailsList)).toFuture
      }(request.request)
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndRequireRegistration(inAmend = true).async {
    implicit request =>

      registrationService.amendRegistration(
        answers = request.userAnswers,
        registration = request.registrationWrapper.etmpDisplayRegistration,
        iossNumber = request.iossNumber
      ).map {
        case Right(_) =>
          Redirect(JourneyRecoveryPage.route(waypoints).url) //TODO -VEI-440 to be implemented
        case Left(error) =>
          val exception = new Exception(error.body)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  private def getTradingNameRows(answers: UserAnswers, waypoints: Waypoints, changePage: ChangeRegistrationPage.type)(implicit messages: Messages) = {
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

  private def getPreviousRegRows(
                                  answers: UserAnswers,
                                  waypoints: Waypoints,
                                  currentPage: ChangeRegistrationPage.type,
                                  existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails]
                                )(implicit messages: Messages) = {

    val previousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(existingPreviousRegistrations)
    val previouslyRegisteredSummaryWithoutActionRow = PreviouslyRegisteredSummary.rowWithoutAction(answers, waypoints)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, currentPage)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, previousRegistrations, waypoints, currentPage)

    val formattedPreviouslyRegisteredSummaryRow = previouslyRegisteredSummaryWithoutActionRow.map { nonOptPreviouslyRegisteredSummaryRow =>
      if (previousRegistrationSummaryRow.isDefined) {
        nonOptPreviouslyRegisteredSummaryRow.withCssClass("govuk-summary-list__row--no-border")
      } else {
        nonOptPreviouslyRegisteredSummaryRow
      }
    }

    val determineRow = if (existingPreviousRegistrations.nonEmpty) {
      formattedPreviouslyRegisteredSummaryRow
    } else {
      previouslyRegisteredSummaryRow
    }

    (determineRow, previousRegistrationSummaryRow)
  }

  private def getFixedEstablishmentRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type)(implicit messages: Messages) = {
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

  private def getBusinessContactRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type)(implicit messages: Messages) = {

    val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, page)

    (formattedContactName, formattedTelephoneNumber, formattedEmailAddress)
  }
}