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

package models.etmp.display

import models.etmp.{EtmpPreviousEuRegistrationDetails, EtmpWebsite}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

case class EtmpDisplaySchemeDetails(
                                     commencementDate: String,
                                     euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails],
                                     contactName: String,
                                     businessTelephoneNumber: String,
                                     businessEmailId: String,
                                     unusableStatus: Boolean,
                                     nonCompliantReturns: Option[String],
                                     nonCompliantPayments: Option[String],
                                     previousEURegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails],
                                     websites: Seq[EtmpWebsite]
                                   )

object EtmpDisplaySchemeDetails {

  private def fromDisplayRegistrationPayload(
                                              commencementDate: String,
                                              euRegistrationDetails: Option[Seq[EtmpDisplayEuRegistrationDetails]],
                                              contactNameOrBusinessAddress: String,
                                              businessTelephoneNumber: String,
                                              businessEmailAddress: String,
                                              unusableStatus: Boolean,
                                              nonCompliantReturns: Option[String],
                                              nonCompliantPayments: Option[String],
                                              previousEURegistrationDetails: Seq[EtmpPreviousEuRegistrationDetails],
                                              websites: Seq[EtmpWebsite]
                                            ): EtmpDisplaySchemeDetails =
    EtmpDisplaySchemeDetails(
      commencementDate = commencementDate,
      euRegistrationDetails = euRegistrationDetails.fold(Seq.empty[EtmpDisplayEuRegistrationDetails])(a => a),
      contactName = contactNameOrBusinessAddress,
      businessTelephoneNumber = businessTelephoneNumber,
      businessEmailId = businessEmailAddress,
      unusableStatus = unusableStatus,
      nonCompliantReturns = nonCompliantReturns,
      nonCompliantPayments = nonCompliantPayments,
      previousEURegistrationDetails = previousEURegistrationDetails,
      websites = websites
    )

  implicit val displaySchemeDetailsReads: Reads[EtmpDisplaySchemeDetails] = {
    (
      (__ \ "commencementDate").read[String] and
        (__ \ "euRegistrationDetails").readNullable[Seq[EtmpDisplayEuRegistrationDetails]] and
        (__ \ "contactDetails" \ "contactNameOrBusinessAddress").read[String] and
        (__ \ "contactDetails" \ "businessTelephoneNumber").read[String] and
        (__ \ "contactDetails" \ "businessEmailAddress").read[String] and
        (__ \ "contactDetails" \ "unusableStatus").read[Boolean] and
        (__ \ "nonCompliantReturns").readNullable[String] and
        (__ \ "nonCompliantPayments").readNullable[String] and
        (__ \ "previousEURegistrationDetails").readNullable[Seq[EtmpPreviousEuRegistrationDetails]].map(_.getOrElse(List.empty)) and
        (__ \ "websites").readNullable[Seq[EtmpWebsite]].map(_.getOrElse(List.empty))
      )(EtmpDisplaySchemeDetails.fromDisplayRegistrationPayload _)
  }

  implicit val etmpDisplaySchemeDetailsWrites: Writes[EtmpDisplaySchemeDetails] = {
    (
      (__ \ "commencementDate").write[String] and
        (__ \ "euRegistrationDetails").write[Seq[EtmpDisplayEuRegistrationDetails]] and
        (__ \ "contactDetails" \ "contactNameOrBusinessAddress").write[String] and
        (__ \ "contactDetails" \ "businessTelephoneNumber").write[String] and
        (__ \ "contactDetails" \ "businessEmailAddress").write[String] and
        (__ \ "contactDetails" \ "unusableStatus").write[Boolean] and
        (__ \ "nonCompliantReturns").writeNullable[String] and
        (__ \ "nonCompliantPayments").writeNullable[String] and
        (__ \ "previousEURegistrationDetails").write[Seq[EtmpPreviousEuRegistrationDetails]] and
        (__ \ "websites").write[Seq[EtmpWebsite]]
      )(etmpDisplaySchemeDetails => Tuple.fromProductTyped(etmpDisplaySchemeDetails))
  }
}
