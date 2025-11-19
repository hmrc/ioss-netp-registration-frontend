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

import controllers.actions.*
import forms.DeleteWebsiteFormProvider
import models.requests.DataRequest
import models.Index
import pages.website.{DeleteWebsitePage, WebsitePage}
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.DeleteWebsiteView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteWebsiteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DeleteWebsiteFormProvider,
                                         view: DeleteWebsiteView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] =
    cc.identifyAndGetData(waypoints.inAmend,checkAmendAccess = Some(DeleteWebsitePage(index))).async {
    implicit request =>
      getWebsite(waypoints, index) {
        website =>
          Ok(view(form, waypoints, index, website)).toFuture
      }
  }

  private def getWebsite(waypoints: Waypoints, index: Index)
                        (block: String => Future[Result])
                        (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(WebsitePage(index)).map {
      website =>
        block(website.site)
    }.getOrElse(Redirect(controllers.website.routes.WebsiteController.onPageLoad(waypoints, index)).toFuture)


  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      getWebsite(waypoints, index) {
        website =>
        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, index, website)).toFuture,

          value =>
            if (value) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(WebsitePage(index)))
                _              <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(DeleteWebsitePage(index).navigate(waypoints, updatedAnswers, updatedAnswers).route)
            } else {
              Redirect(DeleteWebsitePage(index).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
            }
        )
      }
  }
}
