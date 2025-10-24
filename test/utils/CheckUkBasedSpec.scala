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
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks.forAll

class CheckUkBasedSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks{

  ".isUkBasedNetp when provided with " - {
    val ukBasedCountryCode: String = "GB"
    val nonUkCountryCode: String = "DE"
    val arbitraryVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value
    val ukBasedVatInfo: VatCustomerInfo = arbitraryVatInfo.copy(desAddress = arbitraryVatInfo.desAddress.copy(countryCode = ukBasedCountryCode))
    val nonUkVatInfo: VatCustomerInfo = arbitraryVatInfo.copy(desAddress = arbitraryVatInfo.desAddress.copy(countryCode = nonUkCountryCode))
    val arbitraryOtherAddress = arbitraryEtmpOtherAddress.arbitrary.sample.value
    val ukBasedOtherAddress = arbitraryOtherAddress.copy(issuedBy = ukBasedCountryCode)
    val nonUkOtherAddress = arbitraryOtherAddress.copy(issuedBy = nonUkCountryCode)

    "with either VatCustomerInfo or EtmpOtherAddress with Uk based address should return true" in {
      val testCases = Table(
        ("vatInfo", "otherAddress"),
        (Some(ukBasedVatInfo), Some(ukBasedOtherAddress)),
        (Some(ukBasedVatInfo), Some(nonUkOtherAddress)),
        (Some(nonUkVatInfo), Some(ukBasedOtherAddress)),
        (Some(ukBasedVatInfo), None),
        (None, Some(ukBasedOtherAddress)),
      )

      forAll(testCases) { (vatConditions, otherAddressConditions) =>
        val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = vatConditions, otherAddress = otherAddressConditions)

        result mustBe true
      }
    }
    "only VatCustomerInfo WITHOUT Uk based address should return false" in {
      val result = CheckUkBased.isUkBasedNetp(vatCustomerInfo = Some(nonUkVatInfo), otherAddress = None)

      result mustBe false
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
