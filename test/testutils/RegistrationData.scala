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

import base.SpecBase
import formats.Format.eisDateFormatter
import models.Country
import models.etmp.*
import models.etmp.amend.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(eisDateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Some(Seq(arbitrary[EtmpWebsite].sample.value)),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Gen.option(Gen.choose(0, 2).sample.value.toString).sample.value,
    nonCompliantPayments = Gen.option(Gen.choose(0, 2).sample.value.toString).sample.value
  )

  val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    intermediaryDetails = Some(arbitrary[EtmpIntermediaryDetails].sample.value),
    otherAddress = Some(arbitrary[EtmpOtherAddress].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = None
  )

  val etmpAmendRegistrationChangeLog: EtmpAmendRegistrationChangeLog = EtmpAmendRegistrationChangeLog(
    tradingNames = true,
    fixedEstablishments = false,
    contactDetails = true,
    bankDetails = false,
    reRegistration = false,
    otherAddress = false
  )

  val etmpAmendCustomerIdentification: EtmpAmendCustomerIdentification = EtmpAmendCustomerIdentification(
    iossNumber = "IN900123456",
    foreignTaxReference = None
  )

  val amendRegistrationResponse: AmendRegistrationResponse = AmendRegistrationResponse(
    processingDateTime = LocalDateTime.now(),
    formBundleNumber = "123456789",
    iossReference = "IM900123456",
    businessPartner = "Test Business Partner"
  )

  val etmpBankDetails: EtmpBankDetails = {
    val bankDetails = arbitraryBankDetails.arbitrary.sample.value
    EtmpBankDetails(
      accountName = bankDetails.accountName,
      bic = bankDetails.bic,
      iban = bankDetails.iban
    )
  }

  val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = EtmpAmendRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    changeLog = etmpAmendRegistrationChangeLog,
    customerIdentification = etmpAmendCustomerIdentification,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    intermediaryDetails = Some(arbitrary[EtmpIntermediaryDetails].sample.value),
    otherAddress = Some(arbitrary[EtmpOtherAddress].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = None
  )
  
}
