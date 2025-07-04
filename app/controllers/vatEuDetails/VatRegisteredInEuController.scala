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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.vatEuDetails.VatRegisteredInEuFormProvider
import models.UserAnswers
import pages.vatEuDetails.VatRegisteredInEuPage
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.VatRegisteredInEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRegisteredInEuController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: VatRegisteredInEuFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: VatRegisteredInEuView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers.getOrElse(UserAnswers(request.userId)).get(VatRegisteredInEuPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>
          val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
          
          for {
            updatedAnswers <- Future.fromTry(originalAnswers.set(VatRegisteredInEuPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(VatRegisteredInEuPage.navigate(waypoints, originalAnswers, updatedAnswers).route)
      )
  }
}
