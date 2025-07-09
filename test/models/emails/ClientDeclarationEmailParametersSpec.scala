/*
 * Copyright 2024 HM Revenue & Customs
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

package models.emails

import base.SpecBase
import play.api.libs.json.{JsError, Json}

class ClientDeclarationEmailParametersSpec extends SpecBase {

  "ClientDeclarationEmailParameters" - {

    "serialize and deserialize correctly" in {

      val registration = ClientDeclarationEmailParameters(
        recipientName_line1 = "Netp Name",
        intermediary_name = "Intermediary Name",
        activation_code = "123456",
        activation_code_expiry_date = "2024-08-01"
      )


      val json = Json.toJson(registration)
      val expectedJson = Json.parse(
        """
            {
                "recipientName_line1": "Netp Name",
                "intermediary_name": "Intermediary Name",
                "activation_code": "123456",
                "activation_code_expiry_date": "2024-08-01"
            }
          """
      )

      json mustBe expectedJson
      json.as[ClientDeclarationEmailParameters] mustBe registration
    }

    "fail to deserialize when a required field is missing" in {
      val invalidJson = Json.parse(
        """
            {
              "parameters": {
                "recipientName_line1": "Netp Name",
                "intermediary_name": "Intermediary Name",
                "activation_code_expiry_date": "2024-08-01"
              }
            }
          """
      )

      invalidJson.validate[ClientDeclarationEmailParameters] mustBe a[JsError]
    }
  }
}
