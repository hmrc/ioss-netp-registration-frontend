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

package controllers.auth

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.auth.InsufficientEnrolmentsView

import javax.inject.Inject

class AuthController @Inject()(
                                val controllerComponents: MessagesControllerComponents,
                                config: FrontendAppConfig,
                                insufficientEnrolmentsView: InsufficientEnrolmentsView
                              ) extends FrontendBaseController with I18nSupport {

  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(config.allowedRedirectUrls: _*)

  def signOut(): Action[AnyContent] = Action {
    implicit request =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
  }

  def signOutNoSurvey(): Action[AnyContent] = Action {
    _ =>
      Redirect(config.signOutUrl, Map("continue" -> Seq(s"${config.host}${routes.SignedOutController.onPageLoad().path()}")))
  }

  def redirectToLogin(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    Redirect(config.loginUrl,
      Map(
        "origin" -> Seq(config.origin),
        "continue" -> Seq(continueUrl.get(redirectPolicy).url)
      )
    )
  }

  def insufficientEnrolments(): Action[AnyContent] = Action {
    implicit request =>
      Ok(insufficientEnrolmentsView())
  }
}
