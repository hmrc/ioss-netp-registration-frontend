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

package models.ossRegistration

import formats.Format.eisDateFormatter
import models.{Enumerable, WithName}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

case class OssExcludedTrader(
                              vrn: Vrn,
                              exclusionReason: Option[ExclusionReason],
                              effectiveDate: Option[LocalDate],
                              quarantined: Option[Boolean]
                            )

object OssExcludedTrader {

  implicit val reads: Reads[OssExcludedTrader] = {
    (
      (__ \ "vrn").read[Vrn] and
        (__ \ "exclusionReason").readNullable[ExclusionReason] and
        (__ \ "effectiveDate").readNullable[String].map(_.map(d => LocalDate.parse(d, eisDateFormatter))) and
        (__ \ "quarantined").readNullable[Boolean]
      )((vrn, exclusionReason, effectiveDate, quarantined) => OssExcludedTrader(vrn, exclusionReason, effectiveDate, quarantined))
  }

  implicit val readsOpt: Reads[Option[OssExcludedTrader]] = {
    (
      (__ \ "vrn").read[Vrn] and
        (__ \ "excludedTrader").readNullable[JsObject] and
        (__ \ "excludedTrader" \ "exclusionReason").readNullable[ExclusionReason] and
        (__ \ "excludedTrader" \ "effectiveDate").readNullable[String].map(_.map(d => LocalDate.parse(d, eisDateFormatter))) and
        (__ \ "excludedTrader" \ "quarantined").readNullable[Boolean]
      )(
        (vrn, excludedTrader, exclusionReason, effectiveDate, quarantined) =>
          excludedTrader.map { _ =>
            OssExcludedTrader(vrn, exclusionReason, effectiveDate, quarantined)
          }
      )
  }
}


sealed trait ExclusionSource

object HMRC extends ExclusionSource

object TRADER extends ExclusionSource

sealed trait ExclusionReason {
  val exclusionSource: ExclusionSource
  val numberValue: Int
}

object ExclusionReason extends Enumerable.Implicits {

  case object Reversal extends WithName("-1") with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = -1
  }

  case object NoLongerSupplies extends WithName("1") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 1
  }

  case object CeasedTrade extends WithName("2") with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = 2
  }

  case object NoLongerMeetsConditions extends WithName("3") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 3
  }

  case object FailsToComply extends WithName("4") with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = 4
  }

  case object VoluntarilyLeaves extends WithName("5") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 5
  }

  case object TransferringMSID extends WithName("6") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 6
  }

  val values: Seq[ExclusionReason] = Seq(
    Reversal,
    NoLongerSupplies,
    CeasedTrade,
    NoLongerMeetsConditions,
    FailsToComply,
    VoluntarilyLeaves,
    TransferringMSID
  )

  implicit val enumerable: Enumerable[ExclusionReason] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
