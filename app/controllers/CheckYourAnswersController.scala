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

import com.google.inject.Inject
import connectors.RegistrationConnector
import controllers.actions.*
import logging.Logging
import models.CheckMode
import pages.{CheckYourAnswersPage, EmptyWaypoints, ErrorSubmittingPendingRegistrationPage, NonEmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.*
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            registrationConnector: RegistrationConnector,
                                            view: CheckYourAnswersView
                                          )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val thisPage = CheckYourAnswersPage
      val userAnswers = request.userAnswers

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
          VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, userAnswers, thisPage)
        ).flatten
      )

      val websiteSummaryRow = WebsiteSummary.checkAnswersRow(waypoints, userAnswers, thisPage)
      val contactDetailsFullNameRow = BusinessContactDetailsSummary.rowFullName(waypoints, userAnswers, thisPage)
      val contactDetailsTelephoneNumberRow = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, userAnswers, thisPage)
      val contactDetailsEmailAddressRow = BusinessContactDetailsSummary.rowEmailAddress(waypoints, userAnswers, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          websiteSummaryRow,
          contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsEmailAddressRow,
        ).flatten
      )

      val isValid: Boolean = validate()

      Ok(view(waypoints, vatRegistrationDetailsList, list, isValid))
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      registrationConnector.submitPendingRegistration(request.userAnswers).flatMap {
        case Right(_) =>
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
        case Left(error) =>
          logger.error(s"Received an unexpected error on pending registration submission: ${error.body}")
          Redirect(ErrorSubmittingPendingRegistrationPage.route(waypoints).url).toFuture
      }
  }
}
