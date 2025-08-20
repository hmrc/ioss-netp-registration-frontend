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

package controllers.clientDeclarationJourney

import connectors.RegistrationConnector
import controllers.actions.{ClientDataRetrievalAction, ClientIdentifierAction}
import pages.Waypoints
import logging.Logging
import models.{SavedPendingRegistration, UserAnswers}
import models.responses.ErrorResponse
import pages.{ClientBusinessNamePage, JourneyRecoveryPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.IntermediaryDetailsQuery
import services.EmailService
import utils.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.GetClientEmail
import models.emails.EmailSendingResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientResendEmailController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             clientIdentify: ClientIdentifierAction,
                                             clientGetData: ClientDataRetrievalAction,
                                             registrationConnector: RegistrationConnector,
                                             emailService: EmailService,
                                             val controllerComponents: MessagesControllerComponents
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientEmail {

  def onPageLoad(waypoints: Waypoints, uniqueUrlCode: String): Action[AnyContent] = (clientIdentify andThen clientGetData).async {
    implicit request =>

      getClientEmail(waypoints, request.userAnswers) { clientEmail =>

        getIntermediaryName(waypoints, request.userAnswers) { intermediaryName =>

          getClientCompanyName(waypoints, request.userAnswers) { clientCompanyName =>

            registrationConnector.getPendingRegistration(uniqueUrlCode).flatMap {
              case Right(savedPendingRegistration) =>

                sendEmail(
                  savedPendingRegistration,
                  clientEmail,
                  clientCompanyName,
                  intermediaryName
                ).map { _ =>
                  logger.info(s"Successfully resent activation email for uniqueUrlCode: $uniqueUrlCode")
                  Redirect(controllers.clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode))
                }.recover {
                  case ex =>
                    logger.error(s"Failed to resend activation email: ${ex.getMessage}", ex)
                    Redirect(controllers.clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode))
                }

              case Left(errorResponse) =>
                val message = s"Failed to retrieve pending registration for resending email: $errorResponse"
                logger.error(message)
                Future.successful(
                  Redirect(controllers.clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode))
                )
            }
          }
        }
      }
  }
  
  private def sendEmail(
                         submittedRegistration: SavedPendingRegistration,
                         clientEmail: String,
                         clientCompanyName: String,
                         intermediaryName: String
                       )(implicit hc: HeaderCarrier, messages: Messages): Future[EmailSendingResult] = {
    emailService.sendClientActivationEmail(
      intermediary_name = intermediaryName,
      recipientName_line1 = clientCompanyName,
      activation_code_expiry_date = submittedRegistration.activationExpiryDate,
      activation_code = submittedRegistration.uniqueActivationCode,
      emailAddress = clientEmail
    )
  }
  
  private def getIntermediaryName(waypoints: Waypoints, userAnswers: UserAnswers)(block: String => Future[Result]): Future[Result] = {
    userAnswers.get(IntermediaryDetailsQuery).map { intermediaryDetails =>
      block(intermediaryDetails.intermediaryName)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
  }

  private def getClientCompanyName(waypoints: Waypoints, userAnswers: UserAnswers)
                                  (block: String => Future[Result]): Future[Result] = {
    userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>
        vatCustomerInfo.organisationName match {
          case Some(orgName) => block(orgName)
          case _ =>
            vatCustomerInfo.individualName
              .map(block)
              .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
        }

      case _ =>
        userAnswers.get(ClientBusinessNamePage).map { clientBusinessNamePage =>
          block(clientBusinessNamePage.name)
        }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
    }
  }
}