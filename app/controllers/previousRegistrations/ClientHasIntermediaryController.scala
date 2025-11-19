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
import forms.previousRegistrations.ClientHasIntermediaryFormProvider
import models.Index
import pages.Waypoints
import pages.previousRegistrations.ClientHasIntermediaryPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.previousRegistrations.ClientHasIntermediaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientHasIntermediaryController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 formProvider: ClientHasIntermediaryFormProvider,
                                                 view: ClientHasIntermediaryView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] =
    cc.identifyAndGetData(waypoints.inAmend, checkAmendAccess = Some(ClientHasIntermediaryPage(countryIndex, schemeIndex))) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ClientHasIntermediaryPage(countryIndex, schemeIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, countryIndex, schemeIndex))
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, countryIndex, schemeIndex)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientHasIntermediaryPage(countryIndex, schemeIndex), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(ClientHasIntermediaryPage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
