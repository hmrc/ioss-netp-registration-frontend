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

package controllers.vatEuDetails

import controllers.AnswerExtractor
import controllers.actions.*
import forms.vatEuDetails.DeleteEuDetailsFormProvider
import models.Index
import pages.vatEuDetails.DeleteEuDetailsPage
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.{AllEuDetailsRawQuery, DeriveNumberOfEuRegistrations, EuDetailsQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.DeleteEuDetailsView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteEuDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DeleteEuDetailsFormProvider,
                                         view: DeleteEuDetailsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] =
    cc.identifyAndGetData(checkAmendAccess = Some(DeleteEuDetailsPage(countryIndex))) {
    implicit request =>

      getAnswer(waypoints, EuDetailsQuery(countryIndex)) { euDetails =>

        val form: Form[Boolean] = formProvider(euDetails.euCountry)

        Ok(view(form, waypoints, countryIndex, euDetails.euCountry))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      getAnswerAsync(waypoints, EuDetailsQuery(countryIndex)) { euDetails =>

        val form: Form[Boolean] = formProvider(euDetails.euCountry)
        
        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, euDetails.euCountry)).toFuture,

          value =>
            if (value) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(EuDetailsQuery(countryIndex)))
                finalAnswers <- Future.fromTry(cleanupEmptyAnswers(updatedAnswers, DeriveNumberOfEuRegistrations, AllEuDetailsRawQuery))
                _ <- cc.sessionRepository.set(finalAnswers)
              } yield Redirect(DeleteEuDetailsPage(countryIndex).navigate(waypoints, request.userAnswers, finalAnswers).route)
            } else {
              Redirect(DeleteEuDetailsPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
            }
            
        )
      }
  }
}
