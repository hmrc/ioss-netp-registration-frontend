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

package controllers

import models.ClientBusinessName
import models.requests.*
import pages.{ClientBusinessNamePage, JourneyRecoveryPage, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait GetClientCompanyName {

  def getClientCompanyName(waypoints: Waypoints)
                          (block: String => Future[Result])
                          (implicit request: DataRequest[_]): Future[Result] = {
    request.userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>
        vatCustomerInfo.organisationName match {
          case Some(orgName) => block(orgName)
          case _ =>
            vatCustomerInfo.individualName
              .map(block)
              .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
        }

      case _ =>
        request.userAnswers.get(ClientBusinessNamePage).map { companyName =>
          block(companyName.name)
        }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
    }
  }
}