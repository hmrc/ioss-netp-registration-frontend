/*
 * Copyright 2026 HM Revenue & Customs
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

package testutils

import models.core.{Match, TraderId}
import models.previousRegistrations.NonCompliantDetails

object CreateMatchResponse {

  def createMatchResponse(
                           traderId: TraderId = TraderId("IM0987654321"),
                           memberState: String = "DE",
                           exclusionStatusCode: Option[Int] = None,
                           nonCompliantDetails: Option[NonCompliantDetails] = None,
                           exclusionEffectiveDate: Option[String] = None
                         ): Match = Match(
    traderId = traderId,
    intermediary = None,
    memberState = memberState,
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = exclusionEffectiveDate,
    nonCompliantReturns = nonCompliantDetails.flatMap(_.nonCompliantReturns),
    nonCompliantPayments = nonCompliantDetails.flatMap(_.nonCompliantPayments)
  )
}
