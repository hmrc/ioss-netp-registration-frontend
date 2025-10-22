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

import base.SpecBase
import models.domain.VatCustomerInfo
import org.scalatestplus.mockito.MockitoSugar

class CheckUkBasedSpec extends SpecBase with MockitoSugar {

  ".isUkBasedNetp when provided with " - {
    val ukBasedCountryCode: String = "GB"
    val nonUkCountryCode: String = "DE"
    val arbitraryVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value
    val ukBasedVatInfo: VatCustomerInfo = arbitraryVatInfo.copy(desAddress = arbitraryVatInfo.desAddress.copy(countryCode = ukBasedCountryCode))
    val nonUkVatInfo: VatCustomerInfo = arbitraryVatInfo.copy(desAddress = arbitraryVatInfo.desAddress.copy(countryCode = nonUkCountryCode))
    val arbitraryOtherAddress = arbitraryEtmpOtherAddress.arbitrary.sample.value
    val ukBasedOtherAddress = arbitraryOtherAddress.copy(issuedBy = ukBasedCountryCode)
    val nonUkOtherAddress = arbitraryOtherAddress.copy(issuedBy = nonUkCountryCode)

    "VatCustomerInfo & EtmpOtherAddress with Uk based address should return true" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(ukBasedVatInfo), otherAddress = Some(ukBasedOtherAddress))

      result mustBe true
    }
    "VatCustomerInfo WITH a uk address & EtmpOtherAddress WITHOUT a uk address should return false" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(ukBasedVatInfo), otherAddress = Some(nonUkOtherAddress))

      result mustBe false
    }
    "VatCustomerInfo WITHOUT uk address & EtmpOtherAddress WITH a uk address should return false" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(nonUkVatInfo), otherAddress = Some(ukBasedOtherAddress))

      result mustBe false
    }
    "only VatCustomerInfo WITH Uk based address should return true" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(ukBasedVatInfo), otherAddress = None)

      result mustBe true
    }
    "only VatCustomerInfo WITHOUT Uk based address should return false" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(nonUkVatInfo), otherAddress = None)

      result mustBe false
    }
    "only EtmpOtherAddress WITH Uk based address should return true" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = None, otherAddress = Some(ukBasedOtherAddress))

      result mustBe true
    }
    "only EtmpOtherAddress WITHOUT Uk based address should return false" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = None, otherAddress = Some(nonUkOtherAddress))

      result mustBe false
    }
    "Neither VatCustomerInfo or EtmpOtherAddress should throw an error" in {

      val exception = intercept[IllegalStateException] {
        CheckUkBased.isUkBasedNetp(vatCustomerInfo = None, otherAddress = None)

      }
      exception.getMessage mustBe "Unable to identify if client is based in the UK. " +
        "Client requires either Vat Customer Info or an Etmp Other Address from ETMP for amend journey."
    }
  }
}
