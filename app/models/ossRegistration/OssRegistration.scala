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

import models.BankDetails
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate}

case class OssRegistration(
                         vrn: Vrn,
                         registeredCompanyName: String,
                         tradingNames: Seq[String],
                         vatDetails: OssVatDetails,
                         euRegistrations: Seq[OssEuTaxRegistration],
                         contactDetails: OssContactDetails,
                         websites: Seq[String],
                         commencementDate: LocalDate,
                         previousRegistrations: Seq[OssPreviousRegistration],
                         bankDetails: BankDetails,
                         isOnlineMarketplace: Boolean,
                         niPresence: Option[NiPresence],
                         dateOfFirstSale: Option[LocalDate],
                         submissionReceived: Option[Instant],
                         lastUpdated: Option[Instant],
                         excludedTrader: Option[OssExcludedTrader] = None,
                         transferringMsidEffectiveFromDate: Option[LocalDate] = None,
                         nonCompliantReturns: Option[String] = None,
                         nonCompliantPayments: Option[String] = None,
                         adminUse: OssAdminUse
                       )

object OssRegistration {

  implicit val format: Reads[OssRegistration] = Json.reads[OssRegistration]
}