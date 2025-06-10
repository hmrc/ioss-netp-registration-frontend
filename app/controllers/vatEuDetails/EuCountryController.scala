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

import controllers.actions.*
import forms.vatEuDetails.EuCountryFormProvider
import models.{Country, Index, UserAnswers}
import pages.vatEuDetails.EuCountryPage
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.AllEuDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.EuCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuCountryController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: EuCountryFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: EuCountryView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val form: Form[Country] = formProvider(countryIndex, request.userAnswers.get(AllEuDetailsQuery)
        .getOrElse(Seq.empty).map(_.euCountry))

      val preparedForm = request.userAnswers.get(EuCountryPage(countryIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, countryIndex))
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
 
      val form: Form[Country] = formProvider(countryIndex, request.userAnswers.getOrElse(UserAnswers(request.userId)).get(AllEuDetailsQuery)
        .getOrElse(Seq.empty).map(_.euCountry))

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, countryIndex))),

        value =>
          val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
          for {
            updatedAnswers <- Future.fromTry(originalAnswers.set(EuCountryPage(countryIndex), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(EuCountryPage(countryIndex).navigate(waypoints, originalAnswers, updatedAnswers).route)
      )
  }
}
