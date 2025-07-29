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

import models.requests.{ClientOptionalDataRequest, OptionalDataRequest}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait ClientDataRetrievalAction extends ActionTransformer[OptionalDataRequest, ClientOptionalDataRequest]

class ClientDataRetrievalActionImpl @Inject()(
                                               val sessionRepository: SessionRepository
                                             )(implicit val executionContext: ExecutionContext) extends ClientDataRetrievalAction {

  override protected def transform[A](request: OptionalDataRequest[A]): Future[ClientOptionalDataRequest[A]] = {

    sessionRepository.get(request.userId).map { userAnswers =>
      val noneOptionUserAnswers = userAnswers.getOrElse(
        throw new IllegalStateException(
          "userAnswers Are required"
        )
      )

      ClientOptionalDataRequest(request.request, request.userId, noneOptionUserAnswers)
    }
  }
}


