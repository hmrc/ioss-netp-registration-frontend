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
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApplicationCompleteView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               registrationConnector: RegistrationConnector,
                                               frontendAppConfig: FrontendAppConfig,
                                               view: ApplicationCompleteView
                                             )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetClientCompanyName with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>
      getClientCompanyName(waypoints) { clientCompanyName =>

        registrationConnector.getPendingRegistration(request.userAnswers.journeyId).flatMap {
          case Right(savedPendingRegistration) =>
            val clientCodeEntryUrl =
              s"${frontendAppConfig.clientCodeEntryHost}${frontendAppConfig.clientCodeEntryUrl}/${savedPendingRegistration.uniqueUrlCode}"

            for {
              _ <- cc.sessionRepository.clear(request.userId)
            } yield {
              Ok(view(
                clientCompanyName,
                clientCodeEntryUrl,
                savedPendingRegistration.uniqueUrlCode,
                savedPendingRegistration.activationExpiryDate,
                frontendAppConfig.intermediaryYourAccountUrl
              ))
            }

          case Left(errors) =>
            val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given journey ID: $errors."
            val exception: Exception = new Exception(message)
            logger.error(exception.getMessage, exception)
            throw exception
        }
      }
  }
}
