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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckIntermediaryAccessFilterImpl(
                                        iossNumber: Option[String],
                                        registrationConnector: RegistrationConnector
                                       )(implicit val executionContext: ExecutionContext) extends ActionFilter[OptionalDataRequest] with Logging {
  
  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    (request.intermediaryNumber, iossNumber) match {
      case (Some(intermediaryNumber), Some(ioss)) =>
        registrationConnector.displayIntermediaryRegistration(intermediaryNumber).flatMap {
          case Right(intermediaryRegistration) =>
            val availableIossNumbers = intermediaryRegistration.etmpDisplayRegistration.clientDetails.map(_.clientIossID)
            if (availableIossNumbers.contains(ioss)) {
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

      case (Some(intermediaryNumber), None) =>
        None.toFuture

      case _ =>
        logger.warn("No intermediary number present")
        Some(Unauthorized).toFuture
    }
  }
}

class CheckIntermediaryAccessFilterProvider@Inject()(registrationConnector: RegistrationConnector)
                                                    (implicit executionContext: ExecutionContext) {

  def apply(iossNumber: Option[String]): CheckIntermediaryAccessFilterImpl =
    new CheckIntermediaryAccessFilterImpl(iossNumber, registrationConnector)
}
