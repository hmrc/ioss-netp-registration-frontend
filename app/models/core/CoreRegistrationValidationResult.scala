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

package models.core

import play.api.i18n.Lang.logger.logger
import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, OFormat, Reads, Writes}

import java.time.format.DateTimeFormatter

case class CoreRegistrationValidationResult(
                                             searchId: String,
                                             searchIntermediary: Option[String],
                                             searchIdIssuedBy: String,
                                             traderFound: Boolean,
                                             matches: Seq[Match]
                                           )

object CoreRegistrationValidationResult {
  implicit val format: OFormat[CoreRegistrationValidationResult] = Json.format[CoreRegistrationValidationResult]
}


case class Match(
                  matchType: MatchType,
                  traderId: TraderId,
                  intermediary: Option[String],
                  memberState: String,
                  exclusionStatusCode: Option[Int],
                  exclusionDecisionDate: Option[String],
                  exclusionEffectiveDate: Option[String],
                  nonCompliantReturns: Option[Int],
                  nonCompliantPayments: Option[Int]
                ) {
  def getEffectiveDate: String = {
    exclusionEffectiveDate match {
      case Some(date) =>
        date
      case _ =>
        val e = new IllegalStateException(s"MatchType $matchType didn't include an expected exclusion effective date")
        logger.error(s"Must have an Exclusion Effective Date ${e.getMessage}", e)
        throw e
    }
  }
}

object Match {
  
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd")
  val ossDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  
  implicit val format: OFormat[Match] = Json.format[Match]
}

case class TraderId(traderId: String) {
  def isAnIntermediary: Boolean = traderId.toUpperCase.startsWith("IN")
}

object TraderId {
  implicit val traderIdReads: Reads[TraderId] = Reads {
    case JsString(value) => JsSuccess(TraderId(value))
    case _ => JsError("Expected string for TraderId")
  }

  implicit val traderIdWrites: Writes[TraderId] = Writes { traderId =>
    JsString(traderId.traderId)
  }

  implicit val traderIdFormat: Format[TraderId] = Format(traderIdReads, traderIdWrites)
}