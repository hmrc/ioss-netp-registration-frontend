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

import controllers.GetCountry
import controllers.actions.*
import forms.previousRegistrations.PreviousIossNumberFormProvider
import logging.Logging
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.IossRegistrationNumberValidation
import models.{Country, Index, PreviousScheme}
import pages.Waypoints
import pages.previousRegistrations.{PreviousIossNumberPage, PreviousSchemePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousRegistrations.PreviousIossNumberView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousIossNumberController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: PreviousIossNumberFormProvider,
                                              view: PreviousIossNumberView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetCountry{

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>
        val form = formProvider(country)

        val preparedForm = request.userAnswers.get(PreviousIossNumberPage(countryIndex, schemeIndex)) match {
          case None => form
          case Some(value) => form.fill(value.previousSchemeNumber)
        }

        Ok(view(preparedForm, waypoints, countryIndex, schemeIndex, country, getIossHintText(country))).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, schemeIndex: Index): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>

        val form = formProvider(country)
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              formWithErrors, waypoints, countryIndex, schemeIndex, country, getIossHintText(country)))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousIossNumberPage(countryIndex, schemeIndex), PreviousSchemeNumbers(value)))
              updatedAnswersWithPreviousScheme <- Future.fromTry(updatedAnswers.set(
                PreviousSchemePage(countryIndex, schemeIndex), PreviousScheme.IOSSWOI
              ))
              _ <- cc.sessionRepository.set(updatedAnswersWithPreviousScheme)
            } yield Redirect(PreviousIossNumberPage(countryIndex, schemeIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithPreviousScheme).route)
        )

      }
  }

  private def getIossHintText(country: Country): String = {
    IossRegistrationNumberValidation.euCountriesWithIOSSValidationRules.filter(_.country == country).head match {
      case countryWithIossValidation => countryWithIossValidation.messageInput
    }
  }

}
