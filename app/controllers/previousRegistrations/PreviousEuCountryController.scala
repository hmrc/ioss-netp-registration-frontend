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
import forms.previousRegistrations.PreviousEuCountryFormProvider
import models.{Index, UserAnswers}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousIossNumberPage, PreviousOssNumberPage}
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousRegistrations.PreviousEuCountryView
import utils.FutureSyntax.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class PreviousEuCountryController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: PreviousEuCountryFormProvider,
                                       view: PreviousEuCountryView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val form = formProvider(index, request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(Seq.empty).map(_.previousEuCountry))

      val preparedForm = request.userAnswers.get(PreviousEuCountryPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, index))
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      val form = formProvider(index, request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(Seq.empty).map(_.previousEuCountry))

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, index)).toFuture,

        value =>
          val existingCountry = request.userAnswers.get(PreviousEuCountryPage(index))

          val cleanedAnswersTry: Try[UserAnswers] =
            (existingCountry, Some(value)) match {
              case (Some(oldCountry), Some(newCountry)) if oldCountry != newCountry =>
                cleanUp(index, request.userAnswers)
              case _ =>
                Success(request.userAnswers)
            }
          for {
            cleanedAnswers <- Future.fromTry(cleanedAnswersTry)
            updatedAnswers <- Future.fromTry(cleanedAnswers.set(PreviousEuCountryPage(index), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(PreviousEuCountryPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }

  private def cleanUp(index: Index, answers: UserAnswers): Try[UserAnswers] = {
    for {
      remove1 <- answers.remove(PreviousOssNumberPage(index, index))
      cleaned <- remove1.remove(PreviousIossNumberPage(index, index))
    } yield cleaned
  }
}
