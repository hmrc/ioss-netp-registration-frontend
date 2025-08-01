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
import forms.WebsiteFormProvider
import models.{Index, UserAnswers, Website}
import pages.Waypoints
import pages.website.WebsitePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllWebsites
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WebsiteView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WebsiteController @Inject()(
                                   override val messagesApi: MessagesApi,
                                   cc: AuthenticatedControllerComponents,
                                   limitIndex: MaximumIndexFilterProvider,
                                   formProvider: WebsiteFormProvider,
                                   view: WebsiteView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = (cc.identifyAndGetOptionalData andThen limitIndex(index, Constants.maxWebsites)) {
    implicit request =>
      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

      val form = formProvider(index, userAnswers.get(AllWebsites).getOrElse(Seq.empty).map(_.site))

      val preparedForm = userAnswers.get(WebsitePage(index)) match {
        case None => form
        case Some(value) => form.fill(value.site)
      }

      Ok(view(preparedForm, waypoints, index))
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = (cc.identifyAndGetOptionalData andThen limitIndex(index, Constants.maxWebsites)).async {
    implicit request =>

      val form = formProvider(index, request.userAnswers.getOrElse(UserAnswers(request.userId)).get(AllWebsites).getOrElse(Seq.empty).map(_.site))

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, index))),

        value =>
          val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
          for {
            updatedAnswers <- Future.fromTry(originalAnswers.set(WebsitePage(index), Website(value)))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(WebsitePage(index).navigate(waypoints, originalAnswers, updatedAnswers).route)
      )
  }
}
