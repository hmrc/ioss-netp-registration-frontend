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

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class BusinessContactDetailsSpec extends SpecBase {

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

  "BusinessContactDetails" - {

    "must serialise/deserialise from and to a BusinessContactDetails object" in {

      val json = Json.obj(
        "fullName" -> businessContactDetails.fullName,
        "telephoneNumber" -> businessContactDetails.telephoneNumber,
        "emailAddress" -> businessContactDetails.emailAddress
      )

      val expectedResult: BusinessContactDetails = BusinessContactDetails(
        fullName = businessContactDetails.fullName,
        telephoneNumber = businessContactDetails.telephoneNumber,
        emailAddress = businessContactDetails.emailAddress
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[BusinessContactDetails] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[BusinessContactDetails] `mustBe` a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "fullName" -> 12345,
        "telephoneNumber" -> "012345678",
        "emailAddress" -> "a@b.c"
      )

      json.validate[BusinessContactDetails] mustBe a[JsError]
    }
  }
}
