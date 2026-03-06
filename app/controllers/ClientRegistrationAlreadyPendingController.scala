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

package controllers

import controllers.actions.*
import forms.ClientRegistrationAlreadyPendingFormProvider
import logging.Logging
import models.ClientRegistrationAlreadyPending
import models.requests.DataRequest
import models.saveAndComeBack.TaxReferenceInformation

import javax.inject.Inject
import pages.{ClientRegistrationAlreadyPendingPage, ClientVatNumberPage, SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.SaveAndComeBackService
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ClientRegistrationAlreadyPendingView
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class ClientRegistrationAlreadyPendingController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       formProvider: ClientRegistrationAlreadyPendingFormProvider,
                                       cc: AuthenticatedControllerComponents,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ClientRegistrationAlreadyPendingView,
                                       saveAndComeBackService: SaveAndComeBackService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>
      
      getClientCompanyName(waypoints) { clientCompanyName =>

        val preparedForm = request.userAnswers.get(ClientRegistrationAlreadyPendingPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, clientCompanyName)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      getClientCompanyName(waypoints) { clientCompanyName =>

        val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)

        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, clientCompanyName))),

          {
            case ClientRegistrationAlreadyPending.Yes =>
              for {
                savedUserAnswers <- saveAndComeBackService.retrieveSingleSavedUserAnswer(taxReferenceInformation.journeyId, waypoints)
              } yield {
                val continueUrl: String = (savedUserAnswers.data \ "continueUrl").as[String]
                Redirect(Call(GET, continueUrl))
              }
            case ClientRegistrationAlreadyPending.DeleteRegistration =>
              for {
                _ <- saveAndComeBackService.deleteSavedUserAnswers(taxReferenceInformation.journeyId)
              } yield {
                Redirect(ClientVatNumberPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
              }
          }
        )
      }
  }
}
