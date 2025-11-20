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

package controllers.tradingNames

import controllers.actions.*
import forms.tradingNames.DeleteAllTradingNamesFormProvider
import pages.Waypoints
import pages.tradingNames.{DeleteAllTradingNamesPage, HasTradingNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.tradingNames.AllTradingNamesQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.determineRemoveAllItemsAndRedirect
import views.html.tradingNames.DeleteAllTradingNamesView
import utils.AmendWaypoints.AmendWaypointsOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteAllTradingNamesController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         formProvider: DeleteAllTradingNamesFormProvider,
                                         cc: AuthenticatedControllerComponents,
                                         view: DeleteAllTradingNamesView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  val form: Form[Boolean] = formProvider()

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =  cc.identifyAndGetData(waypoints.inAmend, checkAmendAccess = Some(DeleteAllTradingNamesPage)) {
    implicit request =>

      Ok(view(form, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] =  cc.identifyAndGetData(waypoints.inAmend).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>

          determineRemoveAllItemsAndRedirect(
            waypoints = waypoints,
            doRemoveItems = value,
            sessionRepository = sessionRepository,
            query = AllTradingNamesQuery,
            hasItems = HasTradingNamePage,
            deleteAllItemsPage = DeleteAllTradingNamesPage
          )
          
      )
  }
}
