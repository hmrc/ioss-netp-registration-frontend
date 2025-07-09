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

package controllers.previousRegistrations

import controllers.actions.*
import forms.previousRegistrations.DeleteAllPreviousRegistrationsFormProvider
import pages.previousRegistrations.{DeleteAllPreviousRegistrationsPage, PreviouslyRegisteredPage}
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ItemsHelper.determineRemoveAllItemsAndRedirect
import views.html.previousRegistrations.DeleteAllPreviousRegistrationsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteAllPreviousRegistrationsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DeleteAllPreviousRegistrationsFormProvider,
                                         sessionRepository: SessionRepository,
                                         view: DeleteAllPreviousRegistrationsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val preparedForm = request.userAnswers.get(DeleteAllPreviousRegistrationsPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        doRemoveItems =>
          determineRemoveAllItemsAndRedirect(
            waypoints = waypoints,
            doRemoveItems = doRemoveItems,
            sessionRepository = sessionRepository,
            query = AllPreviousRegistrationsQuery,
            hasItems = PreviouslyRegisteredPage,
            deleteAllItemsPage = DeleteAllPreviousRegistrationsPage
          )
      )
  }
}
