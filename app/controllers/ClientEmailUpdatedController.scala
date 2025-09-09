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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.*
import logging.Logging

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.{BusinessContactDetailsPage, Waypoints}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ClientEmailUpdatedView

import scala.concurrent.ExecutionContext

class ClientEmailUpdatedController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       val controllerComponents: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig,
                                       registrationConnector: RegistrationConnector,
                                       view: ClientEmailUpdatedView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging with GetOrganisationOrBusinessName {

  def onPageLoad(waypoints: Waypoints, journeyId: String): Action[AnyContent] = cc.identify.async {
    implicit request =>

        registrationConnector.getPendingRegistration(journeyId).map {
          case Right(pendingRegistration) =>

              val clientCompanyName = getClientCompanyName(pendingRegistration)

              val emailAddress = pendingRegistration.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

              val dashboardUrl = frontendAppConfig.intermediaryYourAccountUrl

              Ok(view(clientCompanyName, emailAddress, dashboardUrl))

          case Left(errors) =>
            val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given journey ID: $errors."
            val exception: Exception = new Exception(message)
            logger.error(exception.getMessage, exception)
            throw exception
      }
  }
}
