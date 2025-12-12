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
import controllers.routes
import logging.Logging
import models.requests.OptionalDataRequest
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import repositories.SessionRepository
import services.UrlBuilderService
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait ClientIdentifierAction extends ActionBuilder[OptionalDataRequest, AnyContent] with ActionFunction[Request, OptionalDataRequest]

class ClientIdentifierActionImpl @Inject()(
                                            val sessionRepository: SessionRepository,
                                            override val authConnector: AuthConnector,
                                            config: FrontendAppConfig,
                                            val parser: BodyParsers.Default,
                                            urlBuilderService: UrlBuilderService
                                          )(implicit val ec: ExecutionContext)
  extends ClientIdentifierAction with AuthorisedFunctions with Logging {

  override protected def executionContext: ExecutionContext = ec

  private lazy val redirectPolicy = (OnlyRelative | AbsoluteWithHostnameFromAllowlist(config.allowedRedirectUrls: _*))

  override def invokeBlock[A](
                               request: Request[A],
                               block: OptionalDataRequest[A] => Future[Result]
                             ): Future[Result] = {


    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId and Retrievals.allEnrolments ) {
      case Some(internalId) ~ enrolments =>
        sessionRepository.get(internalId).flatMap { sessionData =>
          block(OptionalDataRequest(request, internalId, sessionData, None, enrolments))
        }
      case _ =>
        logger.error(s"No Internal ID found to create User ID.\nRequest Body:${request.body}")
        Future.failed(new IllegalStateException("Missing Internal ID"))
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(urlBuilderService.loginContinueUrl(request).get(redirectPolicy).url)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}
