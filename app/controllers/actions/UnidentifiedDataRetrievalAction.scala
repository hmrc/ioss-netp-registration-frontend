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

import models.requests.{IdentifierRequest, OptionalDataRequest, ClientOptionalDataRequest}
import play.api.mvc.{ActionBuilder, ActionFunction, AnyContent, BodyParsers, Request, Result}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait UnidentifiedDataRetrievalAction extends ActionBuilder[ClientOptionalDataRequest, AnyContent] with ActionFunction[Request, ClientOptionalDataRequest]

class UnidentifiedDataRetrievalActionImpl @Inject()(
                                                     val sessionRepository: SessionRepository,
                                                     override val authConnector: AuthConnector,
                                                     val parser: BodyParsers.Default,
                                                   )(implicit val ec: ExecutionContext)
  extends UnidentifiedDataRetrievalAction with AuthorisedFunctions {

  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](
                               request: Request[A],
                               block: ClientOptionalDataRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId) {
      case Some(internalId) =>
        println("\n\nRetrievals.internalId:\n")
        println(internalId)
        sessionRepository.get(internalId).flatMap { sessionData =>
          block(ClientOptionalDataRequest(request, internalId, sessionData, None))
        }
    }
  }
}
