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

package models

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class SavedUserAnswersSpec extends SpecBase {
  
  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  "SavedUserAnswers" - {

    "must serialise/deserialise to and from SavedUserAnswers" - {

      "with all fields present" in {

        val json = Json.obj(
          "journeyId" -> savedUserAnswers.journeyId,
          "data" -> savedUserAnswers.data,
          "intermediaryNumber" -> savedUserAnswers.intermediaryNumber,
          "lastUpdated" -> savedUserAnswers.lastUpdated
        )

        val expectedResult =  SavedUserAnswers(
          journeyId = savedUserAnswers.journeyId,
          data = savedUserAnswers.data,
          intermediaryNumber = savedUserAnswers.intermediaryNumber,
          lastUpdated = savedUserAnswers.lastUpdated
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[SavedUserAnswers] `mustBe` JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[SavedUserAnswers] `mustBe` a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "journeyId" -> savedUserAnswers.journeyId,
          "data" -> savedUserAnswers.data,
          "intermediaryNumber" -> savedUserAnswers.intermediaryNumber,
          "lastUpdated" -> "INVALID"
        )

        expectedJson.validate[SavedUserAnswers] `mustBe` a[JsError]
      }
    }
  }
}
