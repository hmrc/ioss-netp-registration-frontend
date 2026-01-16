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

package controllers.vatEuDetails

import controllers.actions.*
import forms.vatEuDetails.DeleteAllEuDetailsFormProvider
import pages.vatEuDetails.{DeleteAllEuDetailsPage, HasFixedEstablishmentPage}
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ItemsHelper.determineRemoveAllItemsAndRedirect
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.vatEuDetails.DeleteAllEuDetailsView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteAllEuDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DeleteAllEuDetailsFormProvider,
                                         view: DeleteAllEuDetailsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.identifyAndGetData(waypoints.inAmend, checkAmendAccess = Some(DeleteAllEuDetailsPage)) {
    implicit request =>

      val preparedForm = request.userAnswers.get(DeleteAllEuDetailsPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(waypoints.inAmend).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        doRemoveItems =>
          determineRemoveAllItemsAndRedirect(
            waypoints = waypoints,
            doRemoveItems = doRemoveItems,
            sessionRepository = cc.sessionRepository,
            query = AllEuDetailsQuery,
            hasItems = HasFixedEstablishmentPage,
            deleteAllItemsPage = DeleteAllEuDetailsPage
          )
      )
  }
}
