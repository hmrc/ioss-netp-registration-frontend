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

import controllers.GetCountry
import controllers.actions.*
import forms.vatEuDetails.RegistrationTypeFormProvider
import models.{Index, RegistrationType}
import pages.vatEuDetails.RegistrationTypePage
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.RegistrationTypeView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationTypeController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            formProvider: RegistrationTypeFormProvider,
                                            view: RegistrationTypeView
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetCountry{

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getCountryWithIndex(waypoints, countryIndex) { country =>

        val form: Form[RegistrationType] = formProvider(country)
        val preparedForm = request.userAnswers.get(RegistrationTypePage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, countryIndex, country)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getCountryWithIndex(waypoints, countryIndex) { country =>

        val form: Form[RegistrationType] = formProvider(country)
        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, country)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(RegistrationTypePage(countryIndex), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(RegistrationTypePage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
