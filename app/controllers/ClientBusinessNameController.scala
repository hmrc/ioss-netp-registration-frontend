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

package controllers

import controllers.actions.*
import forms.ClientBusinessNameFormProvider

import javax.inject.Inject
import models.ClientBusinessName
import pages.{BusinessBasedInUKPage, ClientBusinessNamePage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.ClientBusinessNameView

import scala.concurrent.{ExecutionContext, Future}

class ClientBusinessNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: ClientBusinessNameFormProvider,
                                        view: ClientBusinessNameView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(waypoints.inAmend, checkAmendAccess = Some(ClientBusinessNamePage)).async {
    implicit request =>
      
      val isUKBased = request.userAnswers.get(BusinessBasedInUKPage).getOrElse(false)
      
      if (isUKBased) {
        val form = formProvider(None)

        val preparedForm = request.userAnswers.get(ClientBusinessNamePage) match {
          case None => form
          case Some(value) => form.fill(value.name)
        }

        Ok(view(preparedForm, waypoints, None, isUKBased)).toFuture
        
      } else {
        
        getCountry(waypoints) { country =>

          val form = formProvider(Some(country))

          val preparedForm = request.userAnswers.get(ClientBusinessNamePage) match {
            case None => form
            case Some(value) => form.fill(value.name)
          }


          Ok(view(preparedForm, waypoints, Some(country), isUKBased)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(inAmend = waypoints.inAmend).async {
    implicit request =>

      val isUKBased = request.userAnswers.get(BusinessBasedInUKPage).getOrElse(false)
      
      if (isUKBased) {

          val form = formProvider(None)
        
          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, None, isUKBased)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientBusinessNamePage, ClientBusinessName(value)))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(ClientBusinessNamePage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        
      } else {

        getCountry(waypoints) { country =>

          val form = formProvider(Some(country))
          
          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, Some(country), isUKBased)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientBusinessNamePage, ClientBusinessName(value)))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(ClientBusinessNamePage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        }
      }
  }
}
