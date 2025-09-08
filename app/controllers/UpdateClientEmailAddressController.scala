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

package controllers

import connectors.RegistrationConnector
import controllers.actions.*
import forms.UpdateClientEmailAddressFormProvider
import logging.Logging

import javax.inject.Inject
import pages.{BusinessContactDetailsPage, JourneyRecoveryPage, UpdateClientEmailAddressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UpdateClientEmailAddressView

import scala.concurrent.ExecutionContext

class UpdateClientEmailAddressController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: UpdateClientEmailAddressFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        registrationConnector: RegistrationConnector,
                                        view: UpdateClientEmailAddressView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetOrganisationOrBusinessName with Logging {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints, journeyId: String): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>

      registrationConnector.getPendingRegistrationsByIntermediaryNumber(request.intermediaryNumber).map {
        case Right(savedPendingRegistrations) =>
          savedPendingRegistrations.find(_.journeyId == journeyId) match
            case Some(registration) =>
              
              val clientCompanyName = getClientCompanyName(registration)
              val emailAddress = registration.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

              val preparedForm = registration.userAnswers.get(UpdateClientEmailAddressPage(journeyId)) match {
                case None => form
                case Some(value) => form.fill(value)
              }
    
              Ok(view(preparedForm, waypoints, journeyId, clientCompanyName, emailAddress))
              
            case None => Redirect(JourneyRecoveryPage.route(waypoints).url)
            
        case Left(errors) =>
          val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given intermediary number: $errors."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  def onSubmit(waypoints: Waypoints, journeyId: String): Action[AnyContent] = ???
}
