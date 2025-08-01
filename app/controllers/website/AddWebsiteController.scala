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

package controllers.website

import config.Constants
import controllers.actions.*
import forms.AddWebsiteFormProvider
import models.Index
import models.requests.DataRequest
import pages.website.AddWebsitePage
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.DeriveNumberOfWebsites
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddWebsiteView
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddWebsiteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: AddWebsiteFormProvider,
                                         view: AddWebsiteView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      getNumberOfWebsites(waypoints) {
        number =>
          val canAddWebsites = number < Constants.maxWebsites
          val filledView = view(
            form = form,
            waypoints = waypoints,
            list = WebsiteSummary.addToListRows(request.userAnswers, waypoints, AddWebsitePage()),
            canAddWebsites = canAddWebsites
          )

          Ok(filledView).toFuture
      }

  }

  private def getNumberOfWebsites(waypoints: Waypoints)(block: Int => Future[Result])
                                 (implicit request: DataRequest[AnyContent]): Future[Result] = {
    request.userAnswers.get(DeriveNumberOfWebsites).map {
      number =>
        block(number)
    }.getOrElse {
      Redirect(controllers.website.routes.WebsiteController.onPageLoad(waypoints, Index(0))).toFuture
    }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getNumberOfWebsites(waypoints) {
        number =>
          val canAddWebsites = number < Constants.maxWebsites

          form.bindFromRequest().fold(
            formWithErrors => {
              val filledView = view(
                form = formWithErrors,
                waypoints = waypoints,
                list = WebsiteSummary.addToListRows(request.userAnswers, waypoints, AddWebsitePage()),
                canAddWebsites = canAddWebsites
              )

              BadRequest(filledView).toFuture
            },

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddWebsitePage(), value))
                _              <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddWebsitePage().navigate(waypoints, updatedAnswers, updatedAnswers).route)
          )

      }
  }
}
