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

package controllers.actions

import connectors.RegistrationConnector
import logging.Logging
import models.requests.OptionalDataRequest
import models.responses.ErrorResponse
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{ActionFilter, Result}
import services.IntermediaryRegistrationService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckIntermediaryAccessFilterImpl(
                                        iossNumber: Option[String],
                                        registrationConnector: RegistrationConnector,
                                        intermediaryRegistrationService: IntermediaryRegistrationService
                                       )(implicit val executionContext: ExecutionContext) extends ActionFilter[OptionalDataRequest] with Logging {
  
  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    iossNumber match {
      case Some(ioss) =>
        intermediaryRegistrationService.getPreviousRegistrations().flatMap {

          case Nil =>
            request.intermediaryNumber match {
              case Some(intermediaryNumber) =>
                registrationConnector.displayIntermediaryRegistration(intermediaryNumber).flatMap {
                  case Right(intermediaryRegistration) =>
                    val availableIossNumbers = intermediaryRegistration.etmpDisplayRegistration.clientDetails.map(_.clientIossID)

                    if(availableIossNumbers.contains(ioss)) {
                      None.toFuture

                    } else {
                      val errorMessage =
                        s"Intermediary $intermediaryNumber tried to access iossNumber $ioss, but they aren't the intermediary of this ioss number"
                      logger.error(errorMessage)
                      Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad().url)).toFuture
                    }

                  case Left(error) =>
                    val errorMessage = s"Error retrieving intermediary registration: ${error.body}"
                    logger.error(errorMessage, error)
                    throw new Exception(errorMessage)
                }

              case None =>
                logger.warn("No intermediary number present")
                Some(Unauthorized).toFuture
            }

          case _ =>
            request.intermediaryNumber match {
              case Some(intermediaryNumber) =>
                val allIntermediaryEnrolments: Seq[String] = findIntermediariesFromEnrolments(request.enrolments)

                findAuthorisedIntermediaryForIossClient(allIntermediaryEnrolments, ioss).flatMap {
                  case Some(authorisedIntermediaryNumber) =>
                    None.toFuture

                  case None =>
                    val errorMessage =
                      s"Intermediary $intermediaryNumber tried to access iossNumber $ioss, but they aren't the intermediary of this ioss number"
                    logger.error(errorMessage)
                    Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad().url)).toFuture
                }

              case None =>
                logger.warn("No intermediary number present")
                Some(Unauthorized).toFuture
            }

        }

      case None =>
        request.intermediaryNumber match {
          case Some(value) => None.toFuture

          case None =>
            logger.warn("No intermediary number present")
            Some(Unauthorized).toFuture
        }
    }
  }

  private def findAuthorisedIntermediaryForIossClient(intermediaryNumbers: Seq[String], iossNumber: String)
                              (implicit hc: HeaderCarrier): Future[Option[String]] = {

    def isAuthorisedToAccessClient(intermediaryNumber: String): Future[Boolean] = {
      registrationConnector.displayIntermediaryRegistration(intermediaryNumber).map { registration =>
        registration.map(_.etmpDisplayRegistration).exists(_.clientDetails.map(_.clientIossID).contains(iossNumber))
      }
    }

    Future.sequence(intermediaryNumbers.map { intermediaryNumber =>
      isAuthorisedToAccessClient(intermediaryNumber)
        .map(isAuthorised => intermediaryNumber -> isAuthorised)
    })
      .map(_.collectFirst { case (intermediaryNumber, true) => intermediaryNumber })
  }

  private def findIntermediariesFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-INT")
      .flatMap(_.identifiers.find(id => id.key == "IntNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }
}

class CheckIntermediaryAccessFilterProvider@Inject()(
                                                      registrationConnector: RegistrationConnector,
                                                      intermediaryRegistrationService: IntermediaryRegistrationService
                                                    )(implicit executionContext: ExecutionContext) {

  def apply(iossNumber: Option[String]): CheckIntermediaryAccessFilterImpl =
    new CheckIntermediaryAccessFilterImpl(iossNumber, registrationConnector, intermediaryRegistrationService)
}
