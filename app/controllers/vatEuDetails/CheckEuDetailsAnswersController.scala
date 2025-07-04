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
import pages.vatEuDetails.CheckEuDetailsAnswersPage
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.CheckEuDetailsAnswersView
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.vatEuDetails.*
import viewmodels.govuk.all.SummaryListViewModel

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckEuDetailsAnswersController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         view: CheckEuDetailsAnswersView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

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

          Ok(view(waypoints, countryIndex, country, list)).toFuture
      }

  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      Redirect(CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
  }
}
