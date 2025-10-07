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

import config.Constants.emailAlertQueuePriority
import connectors.RegistrationConnector
import controllers.actions.*
import forms.UpdateClientEmailAddressFormProvider
import logging.Logging
import pages.{BusinessContactDetailsPage, UpdateClientEmailAddressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmailService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.UpdateClientEmailAddressView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UpdateClientEmailAddressController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: UpdateClientEmailAddressFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        registrationConnector: RegistrationConnector,
                                        emailService: EmailService,
                                        view: UpdateClientEmailAddressView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetOrganisationOrBusinessName with Logging {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints, journeyId: String): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>

      registrationConnector.getPendingRegistration(journeyId).map {
        case Right(savedPendingRegistrations) =>
          
            val clientCompanyName = getClientCompanyName(savedPendingRegistrations)
            val emailAddress = savedPendingRegistrations.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

            val preparedForm = savedPendingRegistrations.userAnswers.get(UpdateClientEmailAddressPage(journeyId)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
  
            Ok(view(preparedForm, waypoints, journeyId, clientCompanyName, emailAddress))
            
        case Left(errors) =>
          val message: String =
            s"Received an unexpected error when trying to retrieve a pending registration for the given journeyId: [$journeyId] with error: $errors."
            
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  def onSubmit(waypoints: Waypoints, journeyId: String): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>

      registrationConnector.getPendingRegistration(journeyId).flatMap {
        case Right(savedPendingRegistration) =>

          val clientCompanyName = getClientCompanyName(savedPendingRegistration)
          val emailAddress = savedPendingRegistration.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, journeyId, clientCompanyName, emailAddress)).toFuture,

            value =>
              registrationConnector.updateClientEmailAddress(journeyId, value).map {
                case Right(updatedClient) =>

                  val intermediaryName = updatedClient.intermediaryDetails.intermediaryName
                  val recipientName = getClientCompanyName(updatedClient)
                  val uniqueActivationCode = updatedClient.uniqueActivationCode
                  val activationExpiryDate = updatedClient.activationExpiryDate
                  val emailAddress = updatedClient.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

                  emailService.sendClientActivationEmail(intermediaryName, recipientName, uniqueActivationCode, activationExpiryDate,
                    emailAddress, alertQueue = Some(emailAlertQueuePriority))
                  
                  Redirect(controllers.routes.ClientEmailUpdatedController.onPageLoad(waypoints, journeyId))

                case Left(errors) =>
                  val message: String =
                    s"Received an unexpected error when trying to update the email address for the given journeyId: [$journeyId] with error: $errors."

                  val exception: Exception = new Exception(message)
                  logger.error(exception.getMessage, exception)
                  throw exception
              }
          )

        case Left(errors) =>
          val message: String =
            s"Received an unexpected error when trying to retrieve a pending registration for the given journeyId: [$journeyId] with error: $errors."

          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }
}
