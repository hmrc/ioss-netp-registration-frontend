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

import config.FrontendAppConfig

import javax.inject.Inject
import models.requests.{ClientOptionalDataRequest, IdentifierRequest, OptionalDataRequest}
import play.api.mvc.{ActionBuilder, ActionTransformer, AnyContent, BodyParsers, Request, Result, WrappedRequest}
import repositories.SessionRepository
import services.{IntermediaryRegistrationService, UrlBuilderService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject()(
                                         val sessionRepository: SessionRepository
                                       )(implicit val executionContext: ExecutionContext) extends DataRetrievalAction {

  override protected def transform[A](request: WrappedRequest[A]): Future[OptionalDataRequest[A]] = {

    request match {
      case ClientOptionalDataRequest(request, userId, userAnswers) =>
        sessionRepository.get(userId).map { userAnswers =>
          val dummyIntermediaryNumb = "Dummy1234567"
          OptionalDataRequest(request, userId, userAnswers)
        }
      case OptionalDataRequest(request, userId, userAnswers) =>
        sessionRepository.get(userId).map { userAnswers =>
          OptionalDataRequest(request, userId, userAnswers)
        }
      case IdentifierRequest(request, userId, enrolments, vrn, intermediaryNumber) =>
        sessionRepository.get(userId).map { userAnswers =>
          OptionalDataRequest(request, userId, userAnswers, intermediaryNumber)
        }
    }
  }
}

trait DataRetrievalAction extends ActionTransformer[WrappedRequest, OptionalDataRequest]

