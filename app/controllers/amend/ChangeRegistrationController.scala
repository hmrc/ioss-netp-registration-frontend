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

import config.Constants.ukCountryCodeAreaPrefix
import controllers.GetClientCompanyName
import controllers.actions.AuthenticatedControllerComponents
import formats.Format.dateFormatter
import logging.Logging
import models.domain.PreviousRegistration
import models.etmp.EtmpPreviousEuRegistrationDetails
import models.{CheckMode, Country, InternationalAddress, UserAnswers}
import pages.*
import pages.amend.{AmendCompletePage, ChangeRegistrationPage}
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
      val clientHasUkAddress: Option[Country] = request.userAnswers.get(ClientCountryBasedPage)
      val countryIsUk: Boolean = clientHasUkAddress match
        case Some(someCountry) => someCountry.code.startsWith(ukCountryCodeAreaPrefix)
        case None => false
      val hasEtmpOtherAddress: Option[InternationalAddress] = request.userAnswers.get(ClientBusinessAddressPage)

      val existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails] = request.registrationWrapper
        .etmpDisplayRegistration.schemeDetails.previousEURegistrationDetails

      val exclusions = request.registrationWrapper.etmpDisplayRegistration.exclusions
      val isExcluded = exclusions.nonEmpty

      val effectiveDate: Option[String] = exclusions.headOption.map { exclusion =>
        exclusion.effectiveDate.format(dateFormatter)
      }

      getClientCompanyName(waypoints) { companyName =>

        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            BusinessBasedInUKSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientHasVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            if (countryIsUk) {
              None
            } else if (isExcluded){
              ClientCountryBasedSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientCountryBasedSummary.row(waypoints, request.userAnswers, thisPage)
            },
            if (isExcluded) {
              ClientTaxReferenceSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientTaxReferenceSummary.row(waypoints, request.userAnswers, thisPage)
            },
            if (!hasEtmpOtherAddress.isDefined) {
              VatRegistrationDetailsSummary.changeRegVatBusinessNameRow(waypoints, request.userAnswers, thisPage, clientBasedInUk)
            } else if (isExcluded) {
              ClientBusinessNameSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientBusinessNameSummary.row(waypoints, request.userAnswers, thisPage)
            },
            ClientHasUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientsNinoNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            if (isExcluded) {
              ClientBusinessAddressSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else if (hasEtmpOtherAddress.isDefined && countryIsUk) {
              ClientBusinessAddressSummary.changeUkBasedRegRow(waypoints, request.userAnswers, thisPage)
            } else if (hasEtmpOtherAddress.isDefined && !countryIsUk) {
              ClientBusinessAddressSummary.row(waypoints, request.userAnswers, thisPage)
            } else {
              VatRegistrationDetailsSummary.changeRegBusinessAddressRow(waypoints, request.userAnswers, thisPage)
            }
          ).flatten
        )

        val (hasTradingNameRow, tradingNameRow) = getTradingNameRows(request.userAnswers, waypoints, thisPage, isExcluded)

        val (previouslyRegisteredRow, previousRegSummaryRow) = getPreviousRegRows(request.userAnswers, waypoints, thisPage, existingPreviousRegistrations, isExcluded)

        val (hasFixedEstablishmentRow, euDetailsSummaryRow) = getFixedEstablishmentRows(waypoints, request.userAnswers, thisPage, isExcluded)

        val (contactNameRow, telephoneNumRow, emailRow) = getBusinessContactRows(waypoints, request.userAnswers, thisPage)

        val websiteSummary = if (isExcluded) {
          WebsiteSummary.checkAnswersRowWithoutAction(waypoints, request.userAnswers)
        } else {
          WebsiteSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
        }

        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            hasTradingNameRow,
            tradingNameRow,
            previouslyRegisteredRow,
            previousRegSummaryRow,
            hasFixedEstablishmentRow,
            euDetailsSummaryRow,
            websiteSummary,
            contactNameRow,
            telephoneNumRow,
            emailRow
          ).flatten
        )

        Ok(view(waypoints, companyName, request.iossNumber, registrationDetailsList, importOneStopShopDetailsList, isExcluded, effectiveDate)).toFuture
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
          Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
        case Left(error) =>
          logger.error(s"Unexpected result on submit: ${error.body}")
          Redirect(controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad())
      }
  }

  private def getTradingNameRows(answers: UserAnswers, waypoints: Waypoints, changePage: ChangeRegistrationPage.type, isExcluded: Boolean)(implicit messages: Messages) = {

    if (isExcluded) {
      val maybeExcludedHasTradingNameSummaryRow = HasTradingNameSummary.rowWithoutAction(answers, waypoints)
      val excludedTradingNameSummaryRow = TradingNameSummary.checkAnswersRowWithoutAction(waypoints, answers)
      val formattedHasTradingNameSummary = maybeExcludedHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
        if (excludedTradingNameSummaryRow.nonEmpty) {
          nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasTradingNameSummaryRow
        }
      }
      (formattedHasTradingNameSummary, excludedTradingNameSummaryRow)
    } else {
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
  }

  private def getPreviousRegRows(
                                  answers: UserAnswers,
                                  waypoints: Waypoints,
                                  currentPage: ChangeRegistrationPage.type,
                                  existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails],
                                  isExcluded: Boolean
                                )(implicit messages: Messages) = {

    val convertedExistingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(existingPreviousRegistrations)
    if (isExcluded) {
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(answers, waypoints)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(answers, convertedExistingPreviousRegistrations, waypoints)
      val formattedPreviouslyRegistered = previouslyRegisteredSummaryRow.map { previouslyRegisteredRow =>
        if (previousRegistrationSummaryRow.nonEmpty) {
          previouslyRegisteredRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          previouslyRegisteredRow
        }
      }
      (formattedPreviouslyRegistered, previousRegistrationSummaryRow)
    } else {
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, currentPage)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, convertedExistingPreviousRegistrations, waypoints, currentPage)

      val formattedPreviouslyRegistered = previouslyRegisteredSummaryRow.map { previouslyRegisteredRow =>
        if (previousRegistrationSummaryRow.nonEmpty) {
          previouslyRegisteredRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          previouslyRegisteredRow
        }
      }

      (formattedPreviouslyRegistered, previousRegistrationSummaryRow)
    }
  }

  private def getFixedEstablishmentRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type, isExcluded: Boolean)(implicit messages: Messages) = {
    if (isExcluded) {
      val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.rowWithoutAction(waypoints, answers)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRowWithoutAction(waypoints, answers)

      val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
        if (euDetailsSummaryRow.nonEmpty) {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
        }
      }
      (formattedHasFixedEstablishmentSummaryRow, euDetailsSummaryRow)
    } else {

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
  }

  private def getBusinessContactRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type)(implicit messages: Messages) = {

    val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, page)

    (formattedContactName, formattedTelephoneNumber, formattedEmailAddress)
  }
}