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
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.BusinessContactDetailsSummary
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            registrationConnector: RegistrationConnector,
                                            view: CheckYourAnswersView
                                          )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val thisPage = CheckYourAnswersPage

      val waypoints: NonEmptyWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))

      val contactDetailsFullNameRow = BusinessContactDetailsSummary.rowFullName(waypoints, request.userAnswers, thisPage)
      val contactDetailsTelephoneNumberRow = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, request.userAnswers, thisPage)
      val contactDetailsEmailAddressRow = BusinessContactDetailsSummary.rowEmailAddress(waypoints, request.userAnswers, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
          contactDetailsEmailAddressRow,
        ).flatten
      )

      Ok(view(waypoints, list))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      registrationConnector.submitPendingRegistration(request.userAnswers).flatMap {
        case Right(_) =>
          Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture

        case Left(error) =>
          logger.error(s"Received an unexpected error on pending registration submission: ${error.body}")
          Redirect(ErrorSubmittingPendingRegistrationPage.route(waypoints).url).toFuture
      }
  }
}
