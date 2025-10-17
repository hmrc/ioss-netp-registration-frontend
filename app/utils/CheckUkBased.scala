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

import config.Constants.ukCountryCodeAreaPrefix
import models.domain.VatCustomerInfo
import models.etmp.EtmpOtherAddress

object CheckUkBased {

  def isUkBasedNetp(vatCustomerInfo: Option[VatCustomerInfo], otherAddress: Option[EtmpOtherAddress]): Boolean = {

    (vatCustomerInfo, otherAddress) match {
      case (Some(vatInfo), Some(address)) =>
        vatInfo.desAddress.countryCode.startsWith(ukCountryCodeAreaPrefix) &&
          address.issuedBy.startsWith(ukCountryCodeAreaPrefix) //TODO- VEI-199, talk to Andrew RE- requirements, can someone be VAT in UK, Address not in UK
      case (Some(vatInfo), None) =>
        vatInfo.desAddress.countryCode.startsWith(ukCountryCodeAreaPrefix)
      case (None, Some(address)) =>
        address.issuedBy.startsWith(ukCountryCodeAreaPrefix)
    }
  }
}
