/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.actions.*
import logging.Logging
import models.CheckMode
import pages.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.summarylist.*
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView
                                          )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData() {
    implicit request =>

      val thisPage = CheckYourAnswersPage
      val userAnswers = request.userAnswers
      val hasUkVatNumber = userAnswers.get(ClientHasVatNumberPage).contains(true)
      val isUKBased = userAnswers.get(BusinessBasedInUKPage).contains(true)

      val waypoints: NonEmptyWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))

      val vatRegistrationDetailsList = SummaryListViewModel(
        rows = Seq(
          BusinessBasedInUKSummary.row(waypoints, userAnswers, thisPage),
          ClientHasVatNumberSummary.row(waypoints, userAnswers, thisPage),
          ClientVatNumberSummary.row(waypoints, userAnswers, thisPage),
          ClientBusinessNameSummary.row(waypoints, userAnswers, thisPage),
          ClientHasUtrNumberSummary.row(waypoints, userAnswers, thisPage),
          ClientUtrNumberSummary.row(waypoints, userAnswers, thisPage),
          ClientsNinoNumberSummary.row(waypoints, userAnswers, thisPage),
          ClientCountryBasedSummary.row(waypoints, userAnswers, thisPage),
          ClientTaxReferenceSummary.row(waypoints, userAnswers, thisPage),
          ClientBusinessAddressSummary.row(waypoints, userAnswers, thisPage),
          if (isUKBased && hasUkVatNumber) VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, userAnswers, thisPage) else None
        ).flatten
      )

      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(userAnswers, waypoints, thisPage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, userAnswers, thisPage)
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(userAnswers, waypoints, thisPage)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(userAnswers, Seq.empty, waypoints, thisPage)
      val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, userAnswers, thisPage)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, userAnswers, thisPage)
      val websiteSummaryRow = WebsiteSummary.checkAnswersRow(waypoints, userAnswers, thisPage)
      val contactDetailsFullNameRow = BusinessContactDetailsSummary.rowFullName(waypoints, userAnswers, thisPage)
      val contactDetailsTelephoneNumberRow = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, userAnswers, thisPage)
      val contactDetailsEmailAddressRow = BusinessContactDetailsSummary.rowEmailAddress(waypoints, userAnswers, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
            if (tradingNameSummaryRow.nonEmpty) {
              hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              hasTradingNameSummaryRow
            }
          },
          tradingNameSummaryRow,
          previouslyRegisteredSummaryRow.map { sr =>
            if (previousRegistrationSummaryRow.isDefined) {
              sr.withCssClass("govuk-summary-list__row--no-border")
            } else {
              sr
            }
          },
          previousRegistrationSummaryRow,
          maybeHasFixedEstablishmentSummaryRow.map { sr =>
            if (euDetailsSummaryRow.nonEmpty) {
              sr.withCssClass("govuk-summary-list__row--no-border")
            } else {
              sr.withCssClass("govuk-summary-list")
            }
          },
          euDetailsSummaryRow,
          websiteSummaryRow,
          contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsEmailAddressRow,
        ).flatten
      )

      val isValid: Boolean = validate()

      Ok(view(waypoints, vatRegistrationDetailsList, list, isValid))
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      getFirstValidationErrorRedirect(waypoints) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(CheckYourAnswersPage.route(waypoints).url).toFuture
        }
        case None =>

          for {
            _ <- cc.sessionRepository.set(request.userAnswers)
          } yield Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
      }
  }
}
