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
import forms.previousRegistrations.PreviousSchemeTypeFormProvider
import models.requests.DataRequest
import models.{Country, Index}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousSchemePage, PreviousSchemeTypePage}
import pages.{JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousRegistrations.AllPreviousSchemesForCountryWithOptionalVatNumberQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousRegistrations.PreviousSchemeView
import utils.FutureSyntax.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousSchemeController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: PreviousSchemeTypeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PreviousSchemeView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) {
        country =>

          val form = request.userAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(countryIndex)) match {
            case Some(previousSchemesDetails) =>

              val previousSchemes = previousSchemesDetails.flatMap(_.previousScheme)
              formProvider(country.name, previousSchemes, schemeIndex)

            case None =>
              formProvider(country.name, Seq.empty, schemeIndex)
          }

          val preparedForm = request.userAnswers.get(PreviousSchemeTypePage(countryIndex, schemeIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, countryIndex, schemeIndex)).toFuture
      }


  }

  private def getPreviousCountry(waypoints: Waypoints, index: Index)
                        (block: Country => Future[Result])
                        (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(PreviousEuCountryPage(index)).map {
      println("HERE")
      country =>
        println("----------- GET PREVIOUS COUNTRY ------------ " + country)
        block(country)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)


  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      getPreviousCountry(waypoints, countryIndex) {
        country =>

          val form = request.userAnswers.get(AllPreviousSchemesForCountryWithOptionalVatNumberQuery(countryIndex)) match {
            case Some(previousSchemesDetails) =>

              val previousSchemes = previousSchemesDetails.flatMap(_.previousScheme)
              formProvider(country.name, previousSchemes, schemeIndex)

            case None =>
              formProvider(country.name, Seq.empty, schemeIndex)
          }

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, countryIndex, schemeIndex)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousSchemeTypePage(countryIndex, schemeIndex), value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(PreviousSchemeTypePage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }
}
