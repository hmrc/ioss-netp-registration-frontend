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

package models.ossRegistration

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}


class OssExcludedTraderSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val ossExcludedTrader: OssExcludedTrader = arbitraryOssExcludedTrader.arbitrary.sample.value

  "OssExcludedTrader" - {

    "must deserialise to an OssExcludedTrader wth all optional fields present" in {

      val json = Json.obj(
        "vrn" -> ossExcludedTrader.vrn,
        "exclusionReason" -> ossExcludedTrader.exclusionReason,
        "effectiveDate" -> ossExcludedTrader.effectiveDate,
        "quarantined" -> ossExcludedTrader.quarantined
      )

      val expectedResult = OssExcludedTrader(
        vrn = ossExcludedTrader.vrn,
        exclusionReason = ossExcludedTrader.exclusionReason,
        effectiveDate = ossExcludedTrader.effectiveDate,
        quarantined = ossExcludedTrader.quarantined
      )

      json.validate[OssExcludedTrader] mustBe JsSuccess(expectedResult)
    }

    "must deserialise to an OssExcludedTrader with all optional fields missing" in {

      val json = Json.obj(
        "vrn" -> ossExcludedTrader.vrn
      )

      val expectedResult = OssExcludedTrader(
        vrn = ossExcludedTrader.vrn,
        exclusionReason = None,
        effectiveDate = None,
        quarantined = None
      )

      json.validate[OssExcludedTrader] mustBe JsSuccess(expectedResult)
    }
  }

  "ExclusionReason" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(ExclusionReason.values)

      forAll(gen) {
        exclusionReason =>
          JsString(exclusionReason.toString).validate[ExclusionReason].asOpt.value mustBe exclusionReason
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!ExclusionReason.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[ExclusionReason] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(ExclusionReason.values)

      forAll(gen) {
        exclusionReason =>

          Json.toJson(exclusionReason) mustBe JsString(exclusionReason.toString)
      }
    }
  }
}
