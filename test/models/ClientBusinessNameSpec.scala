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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}

class ClientBusinessNameSpec extends AnyFreeSpec with Matchers {

  "ClientBusinessName" - {

    "must serialise / deserialise from and to a Client Business name" - {

      "with all optional fields present" in {

        val tradingName: ClientBusinessName = ClientBusinessName("The Scrumptious Cake Company")

        val expectedJson = Json.obj(
          "name" -> "The Scrumptious Cake Company"
        )

        Json.toJson(tradingName) mustBe expectedJson
        expectedJson.validate[ClientBusinessName] mustBe JsSuccess(tradingName)
      }
    }

    "must deserialize from JSON correctly" in {

      val expectedJson = Json.obj(
        "name" -> "The Scrumptious Cake Company"
      )

      val tradingName: ClientBusinessName = ClientBusinessName("The Scrumptious Cake Company")

      expectedJson.validate[ClientBusinessName] mustBe JsSuccess(tradingName)
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[ClientBusinessName] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "name" -> 12345
      )

      expectedJson.validate[ClientBusinessName] mustBe a[JsError]
    }
  }
}
