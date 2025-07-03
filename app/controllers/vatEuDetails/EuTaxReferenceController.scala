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
import forms.vatEuDetails.EuTaxReferenceFormProvider
import models.{Index, Mode}
import pages.vatEuDetails.EuTaxReferencePage
import pages.{Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.EuTaxReferenceView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuTaxReferenceController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: EuTaxReferenceFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: EuTaxReferenceView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {
  

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
    
          getCountryWithIndex(waypoints, countryIndex) { country =>
    
            val form = formProvider(country)
    
            val preparedForm = request.userAnswers.get(EuTaxReferencePage(countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
    
            Ok(view(preparedForm, waypoints, countryIndex, country)).toFuture
          }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EuTaxReferencePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(EuTaxReferencePage, mode, updatedAnswers))
      )
  }
}
