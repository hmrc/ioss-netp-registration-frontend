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

import controllers.GetCountry
import controllers.actions.*
import models.Index
import models.euDetails.EuDetails
import pages.vatEuDetails.{CheckEuDetailsAnswersPage, EuCountryPage}
import pages.{Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.EuDetailsCompletionChecks.*
import views.html.vatEuDetails.CheckEuDetailsAnswersView
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.vatEuDetails.*
import viewmodels.govuk.all.SummaryListViewModel

import javax.inject.Inject

class CheckEuDetailsAnswersController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         view: CheckEuDetailsAnswersView
                                 ) extends FrontendBaseController with I18nSupport with GetCountry with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      getCountryWithIndex(waypoints, countryIndex) {
        country =>

          val thisPage = CheckEuDetailsAnswersPage(countryIndex)
          val list = SummaryListViewModel(
            rows = Seq(
              EuCountrySummary.row(waypoints, request.userAnswers, countryIndex, thisPage),
              TradingNameAndBusinessAddressSummary.row(waypoints, request.userAnswers, countryIndex, thisPage),
              RegistrationTypeSummary.row(waypoints, request.userAnswers, countryIndex, thisPage),
              EuVatNumberSummary.row(waypoints, request.userAnswers, countryIndex, thisPage),
              EuTaxReferenceSummary.row(waypoints, request.userAnswers, countryIndex, thisPage),
            ).flatten
          )
          
          withCompleteDataModel[EuDetails](
            index = countryIndex,
            data = getIncompleteEuDetails _,
            onFailure = (incomplete: Option[EuDetails]) => {
              Ok(view(waypoints, countryIndex, country, list, incomplete.isDefined))
            }) {
            Ok(view(waypoints, countryIndex, country, list))
          }.toFuture
      }

  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      withCompleteDataModel[EuDetails](
        index = countryIndex,
        data = getIncompleteEuDetails _,
        onFailure = (_: Option[EuDetails]) => {
          if (!incompletePromptShown) {
            Redirect(CheckEuDetailsAnswersPage(countryIndex).route(waypoints).url)
          } else {
            val updatedWaypoints: Waypoints = waypoints
              .setNextWaypoint(Waypoint(CheckEuDetailsAnswersPage(countryIndex), waypoints.currentMode, CheckEuDetailsAnswersPage(countryIndex).urlFragment))
            incompleteEuDetailsRedirect(updatedWaypoints) match {
              case Some(redirectResult) => redirectResult
              case _ => Redirect(EuCountryPage(countryIndex).route(updatedWaypoints).url)
            }
          }
        }) {
        Redirect(CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route)
      }.toFuture
  }
}
