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

package controllers.previousRegistrations

import models.{Index, UserAnswers}
import models.previousRegistrations.NonCompliantDetails
import queries.previousRegistrations.NonCompliantDetailsQuery
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait PreviousNonComplianceAnswers {

  def setNonCompliantDetailsAnswers(
                                             countryIndex: Index,
                                             schemeIndex: Index,
                                             maybeNonCompliantDetails: Option[NonCompliantDetails],
                                             updatedAnswers: UserAnswers
                                           ): Future[UserAnswers] = {
    maybeNonCompliantDetails match {
      case Some(nonCompliantDetails) =>
        Future.fromTry(updatedAnswers.set(NonCompliantDetailsQuery(countryIndex, schemeIndex), nonCompliantDetails))
      case _ =>
        updatedAnswers.toFuture
    }
  }

}
