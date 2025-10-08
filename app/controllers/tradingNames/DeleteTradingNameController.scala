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

import controllers.AnswerExtractor
import controllers.actions.*
import forms.tradingNames.DeleteTradingNameFormProvider
import models.Index
import pages.Waypoints
import pages.tradingNames.{DeleteTradingNamePage, TradingNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.tradingNames.DeleteTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteTradingNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        sessionRepository: SessionRepository,
                                        formProvider: DeleteTradingNameFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: DeleteTradingNameView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {
  
  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = (identify andThen getData andThen requireData()) {
    implicit request =>

      getAnswer(waypoints, TradingNamePage(index)) {
        tradingName =>

          Ok(view(form, waypoints, index, tradingName))
      }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = (identify andThen getData andThen requireData()).async {
    implicit request =>

      getAnswerAsync(waypoints, TradingNamePage(index)) {
        tradingName =>

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, index, tradingName)).toFuture,

            value =>
              if (value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(TradingNamePage(index)))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteTradingNamePage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              } else {
                Redirect(DeleteTradingNamePage(index).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
              }
          )
      }
  }
}
