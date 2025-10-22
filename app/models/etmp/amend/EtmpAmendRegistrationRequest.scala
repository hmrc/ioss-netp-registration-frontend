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

package models.etmp.amend

import models.UserAnswers
import models.etmp.*
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import models.etmp.display.{EtmpDisplayRegistration, EtmpDisplaySchemeDetails}
import play.api.libs.json.{Json, OFormat}
import java.time.LocalDate

case class EtmpAmendRegistrationRequest(
                                         administration: EtmpAdministration,
                                         changeLog: EtmpAmendRegistrationChangeLog,
                                         customerIdentification: EtmpAmendCustomerIdentification,
                                         tradingNames: Seq[EtmpTradingName],
                                         intermediaryDetails: Option[EtmpIntermediaryDetails],
                                         otherAddress: Option[EtmpOtherAddress],
                                         schemeDetails: EtmpSchemeDetails,
                                         bankDetails: Option[EtmpBankDetails]
                                       )

object EtmpAmendRegistrationRequest {

  implicit val format: OFormat[EtmpAmendRegistrationRequest] = Json.format[EtmpAmendRegistrationRequest]

  def buildEtmpAmendRegistrationRequest(
                                         answers: UserAnswers,
                                         registration: EtmpDisplayRegistration,
                                         commencementDate: LocalDate,
                                         iossNumber: String,
                                         rejoin: Boolean = false
                                       ): EtmpAmendRegistrationRequest = {

    val etmpRegistrationRequest = buildEtmpRegistrationRequest(answers, commencementDate)

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSIntAmend),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = registration.tradingNames != etmpRegistrationRequest.tradingNames,
        fixedEstablishments = registration.schemeDetails.euRegistrationDetails != etmpRegistrationRequest.schemeDetails.euRegistrationDetails,
        contactDetails = contactDetailsDiff(registration.schemeDetails, etmpRegistrationRequest.schemeDetails),
        bankDetails = false,
        reRegistration = rejoin,
        otherAddress = registration.otherAddress != etmpRegistrationRequest.otherAddress
      ),
      customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
      tradingNames = etmpRegistrationRequest.tradingNames,
      intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
      otherAddress = etmpRegistrationRequest.otherAddress,
      schemeDetails = etmpRegistrationRequest.schemeDetails,
      bankDetails = etmpRegistrationRequest.bankDetails
    )
  }

  private def contactDetailsDiff(registrationSchemeDetails: EtmpDisplaySchemeDetails, amendSchemeDetails: EtmpSchemeDetails): Boolean = {
    registrationSchemeDetails.contactName != amendSchemeDetails.contactName ||
      registrationSchemeDetails.businessTelephoneNumber != amendSchemeDetails.businessTelephoneNumber ||
      registrationSchemeDetails.businessEmailId != amendSchemeDetails.businessEmailId
  }
}