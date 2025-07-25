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

import controllers.GetClientCompanyName
import controllers.actions.*
import forms.tradingNames.HasTradingNameFormProvider
import pages.Waypoints
import pages.tradingNames.HasTradingNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.tradingNames.HasTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasTradingNameController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         sessionRepository: SessionRepository,
                                         val controllerComponents: MessagesControllerComponents,
                                         formProvider: HasTradingNameFormProvider,
                                         view: HasTradingNameView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      getClientCompanyName(waypoints) {
        companyName =>
          val preparedForm = request.userAnswers.get(HasTradingNamePage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, companyName)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      getClientCompanyName(waypoints) {
        companyName =>

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, companyName)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(HasTradingNamePage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(HasTradingNamePage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }
}
