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

import logging.Logging
import models.requests.{DataRequest, IntermediaryDataRequest}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO -> Required???
class IntermediaryRequiredActionImpl @Inject()()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[DataRequest, IntermediaryDataRequest] with Logging {

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, IntermediaryDataRequest[A]]] = {
    request.intermediaryNumber match {
      case intermediaryNumber =>
        request.registrationWrapper match {
          case Some(wrapper) =>
            Right(IntermediaryDataRequest(
              request = request.request,
              userId = request.userId,
              userAnswers = request.userAnswers,
              intermediaryNumber = intermediaryNumber,
              registrationWrapper = wrapper
            )).toFuture

          case None =>
            logger.warn("No registration wrapper present in amend mode")
            Left(Unauthorized).toFuture
        }
    }
  }
}

class IntermediaryRequiredAction @Inject()()(implicit executionContext: ExecutionContext) {
  def apply(): IntermediaryRequiredActionImpl = {
    new IntermediaryRequiredActionImpl()
  }
}