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

package utils

import models.UserAnswers
import pages.{BusinessContactDetailsPage, JourneyRecoveryPage, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait GetClientEmail {

  def getClientEmail(waypoints: Waypoints, userAnswers: UserAnswers)
                    (block: String => Future[Result]): Future[Result] = {
    userAnswers.get(BusinessContactDetailsPage).map { businessContactDetails =>
      block(businessContactDetails.emailAddress)
    }.getOrElse {
      Redirect(JourneyRecoveryPage.route(waypoints)).toFuture
    }
  }
}