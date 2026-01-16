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

package models.domain

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class VatCustomerInfoSpec extends SpecBase {

  private val vatCustomerInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

  "VatCustomerInfo" - {

    "must serialise / deserialise from and to a Vat Customer Info object" - {

      "with all optional fields present" in {

        val expectedJson = Json.obj(
          "desAddress" -> vatCustomerInfo.desAddress,
          "registrationDate" -> vatCustomerInfo.registrationDate,
          "organisationName" -> vatCustomerInfo.organisationName,
          "individualName" -> vatCustomerInfo.individualName,
          "singleMarketIndicator" -> vatCustomerInfo.singleMarketIndicator
        )

        Json.toJson(vatCustomerInfo) mustBe expectedJson
        expectedJson.validate[VatCustomerInfo] mustBe JsSuccess(vatCustomerInfo)
      }
    }

    "with all optional fields absent" in {

      val json = Json.obj(
        "desAddress" -> vatCustomerInfo.desAddress,
        "registrationDate" -> vatCustomerInfo.registrationDate,
        "singleMarketIndicator" -> vatCustomerInfo.singleMarketIndicator
      )

      val expectedResult: VatCustomerInfo = VatCustomerInfo(
        desAddress = vatCustomerInfo.desAddress,
        registrationDate = vatCustomerInfo.registrationDate,
        organisationName = None,
        individualName = None,
        singleMarketIndicator = vatCustomerInfo.singleMarketIndicator,
        deregistrationDecisionDate = None
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[VatCustomerInfo] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[VatCustomerInfo] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "singleMarketIndicator" -> 12345
      )

      expectedJson.validate[VatCustomerInfo] mustBe a[JsError]
    }
  }
}
