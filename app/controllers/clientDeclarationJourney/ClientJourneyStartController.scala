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

package controllers.clientDeclarationJourney

import connectors.RegistrationConnector
import controllers.actions.ClientIdentifierAction
import logging.Logging
import models.IntermediaryDetails
import pages.Waypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.IntermediaryDetailsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientJourneyStartController @Inject()(
                                              clientIdentify: ClientIdentifierAction,
                                              registrationConnector: RegistrationConnector,
                                              sessionRepository: SessionRepository,
                                              val controllerComponents: MessagesControllerComponents,
                                            )(implicit executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(waypoints: Waypoints, uniqueUrlCode: String): Action[AnyContent] = clientIdentify.async {
    implicit request =>

      registrationConnector.getPendingRegistration(uniqueUrlCode).flatMap {
        case Right(savedPendingRegistration) =>

          for {
            updatedAnswers <- Future.fromTry(
              savedPendingRegistration.userAnswers.copy(request.userId).set(
                IntermediaryDetailsQuery,
                IntermediaryDetails(
                  savedPendingRegistration.intermediaryDetails.intermediaryNumber,
                  savedPendingRegistration.intermediaryDetails.intermediaryName
                )
              )
            )
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode))

        case Left(error) =>
          val message: String =
            s"Received an unexpected error when trying to retrieve a pending registration for the given unique Url Code: $uniqueUrlCode, \n Errors: $error."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }

  }

}
