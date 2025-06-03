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

import models.requests.OptionalDataRequest
import pages.clientDeclarationJourney.ClientDeclarationPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientDeclarationFilter()
                             (implicit val executionContext: ExecutionContext) extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] =
    request.userAnswers match {
      case Some(userAnswers) =>
        userAnswers.get(ClientDeclarationPage) match {
          case Some(value) if !value =>
            Future.successful(Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad())))

          case Some(value) =>
            Future.successful(None)

          case None =>
            Future.successful(Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad())))
        }
      case None =>
        Future.successful(Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad())))
    }
}

class ClientDeclarationFilterProvider @Inject()(implicit executionContext: ExecutionContext) {
  def apply(): ClientDeclarationFilter = new ClientDeclarationFilter()
}