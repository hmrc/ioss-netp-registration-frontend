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

import controllers.routes
import logging.Logging
import models.requests.IdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.Inject

class CheckIntermediaryEnrolmentActionImpl()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, IdentifierRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    request.intermediaryNumber match {
      case Some(_) =>
        Right(request).toFuture
      case None =>
        logger.warn(s"User ${request.userId} does not have intermediary enrolment")
        Left(Redirect(routes.CannotUseNotAnIntermediaryController.onPageLoad())).toFuture
    }
  }
}

class CheckIntermediaryEnrolmentAction @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(): CheckIntermediaryEnrolmentActionImpl = {
    new CheckIntermediaryEnrolmentActionImpl()
  }
}
