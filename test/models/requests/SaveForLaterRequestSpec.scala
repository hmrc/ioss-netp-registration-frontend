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

package models.requests

import base.SpecBase
import models.SaveForLaterRequest
import play.api.libs.json.{JsError, JsSuccess, Json}

class SaveForLaterRequestSpec extends SpecBase {

  private val saveForLaterRequest: SaveForLaterRequest = arbitrarySaveForLaterRequest.arbitrary.sample.value

  "SaveForLaterRequest" - {

    "must serialise/deserialise to and from SaveForLaterRequest" - {

      "with all fields present" in {

        val json = Json.obj(
          "journeyId" -> saveForLaterRequest.journeyId,
          "data" -> saveForLaterRequest.data,
          "intermediaryNumber" -> saveForLaterRequest.intermediaryNumber
        )

        val expectedResult = SaveForLaterRequest(
          journeyId = saveForLaterRequest.journeyId,
          data = saveForLaterRequest.data,
          intermediaryNumber = saveForLaterRequest.intermediaryNumber
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[SaveForLaterRequest] `mustBe` JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[SaveForLaterRequest] `mustBe` a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "journeyId" -> 123456,
          "data" -> saveForLaterRequest.data,
          "intermediaryNumber" -> saveForLaterRequest.intermediaryNumber
        )

        expectedJson.validate[SaveForLaterRequest] `mustBe` a[JsError]
      }
    }
  }
}
