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

import logging.Logging
import models.UserAnswers
import models.requests.{ClientOptionalDataRequest, OptionalDataRequest}
import pages.EmptyWaypoints
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import queries.ClientUrlCodeQuery
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait ClientDataRetrievalAction extends ActionRefiner[OptionalDataRequest, ClientOptionalDataRequest]

class ClientDataRetrievalActionImpl @Inject()(
                                               val sessionRepository: SessionRepository
                                             )(implicit val executionContext: ExecutionContext) extends ClientDataRetrievalAction with Logging {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, ClientOptionalDataRequest[A]]] = {


    val possiblyUniqueUrlCode = request.uri.split('/').lastOption.getOrElse("")

    sessionRepository.get(request.userId).flatMap {
      case None =>
        Future.successful(
          Left(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyStartController.onPageLoad(EmptyWaypoints, possiblyUniqueUrlCode)))
        )


      case Some(userAnswers) =>
        userAnswers.get(ClientUrlCodeQuery) match {
          case Some(clientUrlCode) => Future.successful(Right(ClientOptionalDataRequest(request.request, request.userId, userAnswers)))
          case None =>
            for {
              updatedUserAnswers <- Future.fromTry(userAnswers.set(ClientUrlCodeQuery, possiblyUniqueUrlCode))
              _ <- sessionRepository.set(updatedUserAnswers)
            } yield {
              val result: Right[Nothing, ClientOptionalDataRequest[A]] = Right(ClientOptionalDataRequest(request.request, request.userId, updatedUserAnswers))
              result
            }
        }
    }
  }
}
