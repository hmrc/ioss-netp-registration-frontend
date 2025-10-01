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
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.previousRegistrations.PreviousRegistrationDetailsWithOptionalVatNumber
import models.requests.DataRequest
import models.Index
import pages.previousRegistrations.DeletePreviousRegistrationPage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistrations.{PreviousRegistrationQuery, PreviousRegistrationWithOptionalVatNumberQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousRegistrations.DeletePreviousRegistrationView
import utils.FutureSyntax.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeletePreviousRegistrationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: DeletePreviousRegistrationFormProvider,
                                       view: DeletePreviousRegistrationView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  protected val controllerComponents: MessagesControllerComponents = cc
  
  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>
      getPreviousRegistration(waypoints, index) {
        details =>
            Ok(view(form, waypoints, index, details.previousEuCountry.name)).toFuture
      }
  }

  private def getPreviousRegistration(waypoints: Waypoints, index: Index)
                                     (block: PreviousRegistrationDetailsWithOptionalVatNumber => Future[Result])
                                     (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousRegistrationWithOptionalVatNumberQuery(index)).map {
      details =>
        block(details)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
    
    
  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>
      getPreviousRegistration(waypoints, index) {
        details =>
            saveAndRedirect(waypoints, index, details.previousEuCountry.name)
          
      }
  }

  private def saveAndRedirect(
                               waypoints: Waypoints,
                               index: Index,
                               countryName: String)
                             (implicit request: DataRequest[AnyContent]): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, waypoints, index, countryName))),

      value =>
        if (value) {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(PreviousRegistrationQuery(index)))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(DeletePreviousRegistrationPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        } else {
          Redirect(DeletePreviousRegistrationPage(index).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
        }
    )
  }
}
