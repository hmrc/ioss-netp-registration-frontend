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

package controllers.actions

import connectors.RegistrationConnector
import controllers.routes
import logging.Logging
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRequiredActionImpl @Inject()(registrationConnector: RegistrationConnector,
                                       isInAmendMode: Boolean)(implicit val executionContext: ExecutionContext) extends ActionRefiner[OptionalDataRequest, DataRequest] with Logging {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = {

    val intermediaryNumber = request.intermediaryNumber.getOrElse {
      logger.warn(s"The intermediary number is required for ${request.userId} to complete journey")
      throw new IllegalStateException(s"The Intermediary Number is required for ${request.userId}")
    }

    request.userAnswers match {
      case None =>
        Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))

      case Some(data) =>
        val eventualMaybeRegistrationWrapper = {
          if (isInAmendMode) {
             implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.session)
             registrationConnector.getIossRegistration(intermediaryNumber)(hc).flatMap {
               case Left(error) => Future.failed(new RuntimeException(s"Failed to retrieve registration: ${error.body}"))
               case Right(registrationWrapper) => Future.successful(Some(registrationWrapper))
             }
          } else {
            Future.successful(None)
          }
        }

        eventualMaybeRegistrationWrapper.map { maybeWrapper =>
          Right(DataRequest(
            request.request,
            request.userId,
            data,
            intermediaryNumber,
            maybeWrapper
          ))
        }
    }
  }
}

class DataRequiredAction @Inject()(
                                            registrationConnector: RegistrationConnector
                                          )(implicit ec: ExecutionContext){

  def apply(isInAmendMode: Boolean = false): ActionRefiner[OptionalDataRequest, DataRequest] = {
    new DataRequiredActionImpl(registrationConnector, isInAmendMode)
  }
}
