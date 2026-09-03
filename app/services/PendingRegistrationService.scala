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

package services

import connectors.RegistrationConnector
import logging.Logging
import models.domain.VatCustomerInfo
import models.{SavedPendingRegistration, SavedPendingRegistrationWithUserAnswers, UserAnswers}
import models.etmp.EtmpIdType
import models.responses.ErrorResponse
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{ClientVatNumberPage, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PendingRegistrationService @Inject()(
                                            registrationConnector: RegistrationConnector
                                          )(implicit ec: ExecutionContext) extends Logging {

  def checkPendingRegistrationDuplication(
                                           idType: EtmpIdType,
                                           idValue: String,
                                           intermediaryNumber: String,
                                           waypoints: Waypoints
                                         )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    registrationConnector.getPendingRegistrationsByCustomerIdentification(idType, idValue).map {
      case Right(pendingRegistrations) =>
        pendingRegistrations.find(_.intermediaryDetails.intermediaryNumber == intermediaryNumber) match {
          case Some(pendingRegistration) =>
            Some(Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, pendingRegistration.journeyId)))
          case None if pendingRegistrations.nonEmpty =>
            Some(Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad()))
          case None =>
            None
        }
      case Left(_) => None
    }
  }

  def getPendingRegistration(journeyIdOrUrlCode: String, userId: String)(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SavedPendingRegistrationWithUserAnswers]] = {

      registrationConnector.getPendingRegistration(journeyIdOrUrlCode).flatMap {
        case Right(pendingRegistration) =>
          toSavedPendingRegistrationWithUserAnswers(pendingRegistration, userId)
        case _ =>
          val message: String = s"Unable to retrieve pending registration for journey ID or URL code $journeyIdOrUrlCode"
          logger.error(message)
          val exception: IllegalStateException = new IllegalStateException(message)
          throw exception
      }
  }

  private def toSavedPendingRegistrationWithUserAnswers(
                                                         savedPendingRegistration: SavedPendingRegistration,
                                                         userId: String
                                                       )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SavedPendingRegistrationWithUserAnswers]] = {
    val userAnswers = UserAnswers(
      userId,
      savedPendingRegistration.journeyId,
      savedPendingRegistration.userAnswersData,
      None
    )

    def buildRegistration(
                           answers: UserAnswers
                         ): SavedPendingRegistrationWithUserAnswers =
      SavedPendingRegistrationWithUserAnswers(
        savedPendingRegistration.journeyId,
        savedPendingRegistration.uniqueUrlCode,
        answers,
        savedPendingRegistration.lastUpdated,
        savedPendingRegistration.uniqueActivationCode,
        savedPendingRegistration.intermediaryDetails
      )


    val maybeVatNumber = userAnswers.get(ClientVatNumberPage)

    maybeVatNumber match {
      case Some(vatNumber) =>
        registrationConnector.getVatCustomerInfo(vatNumber).map {
          case Right(vatInfo) =>

            val refreshedAnswers = userAnswers.copy(vatInfo = Some(vatInfo))
            val answers = if (vatInfo.partOfVatGroup) {
              removeFixedEstablishmentAnswers(refreshedAnswers).get
            } else {
              refreshedAnswers.get(HasFixedEstablishmentPage) match {
                case Some(_) =>
                  refreshedAnswers
                case None =>
                  refreshedAnswers.set(HasFixedEstablishmentPage, false).get
              }
            }

            Right(buildRegistration(answers))

          case Left(errorResponse) =>
            logger.warn(
              s"Unable to retrieve VAT customer information for journey " +
                s"[${savedPendingRegistration.journeyId}]: $errorResponse" +
                s"Continuing without VAT information."
            )
            Right(buildRegistration(userAnswers))
        }
      case _ =>
        Future.successful(
          Right(buildRegistration(userAnswers))
        )
    }
  }

  def updateClientEmailAddress(
                                journeyId: String,
                                newEmailAddress: String,
                                userId: String
                              )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SavedPendingRegistrationWithUserAnswers]] = {
    registrationConnector
      .updateClientEmailAddress(journeyId, newEmailAddress)
      .flatMap {
        case Right(savedPendingRegistration) =>
          toSavedPendingRegistrationWithUserAnswers(
            savedPendingRegistration,
            userId
          )

        case Left(errorResponse) =>
          logger.error(
            s"Unable to update client email address for journeyId " +
              s"[$journeyId]: $errorResponse"
          )

          Future.successful(Left(errorResponse))
      }
  }

  private def removeFixedEstablishmentAnswers(userAnswers: UserAnswers): Try[UserAnswers] = {
    for {
      withoutHasFixedEstablishments <- userAnswers.remove(HasFixedEstablishmentPage)
      withoutEuDetails <- withoutHasFixedEstablishments.remove(AllEuDetailsQuery)
    } yield withoutEuDetails
  }
}
