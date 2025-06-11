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

package controllers

import controllers.actions.*
import forms.ClientTaxReferenceFormProvider

import javax.inject.Inject
import pages.ClientTaxReferencePage
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.ClientTaxReferenceView

import scala.concurrent.{ExecutionContext, Future}

class ClientTaxReferenceController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: ClientTaxReferenceFormProvider,
                                              view: ClientTaxReferenceView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getCountry(waypoints) { country =>

        val form = formProvider(country)

        val preparedForm = request.userAnswers.get(ClientTaxReferencePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, country)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getCountry(waypoints) { country =>

        val form = formProvider(country)

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, country)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientTaxReferencePage, value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(ClientTaxReferencePage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
