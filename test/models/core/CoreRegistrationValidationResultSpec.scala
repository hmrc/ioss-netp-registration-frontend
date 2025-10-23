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

package models.core

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, Json, JsSuccess}

import java.time.LocalDate

class CoreRegistrationValidationResultSpec extends AnyFreeSpec with Matchers with SpecBase {

  "CoreRegistrationValidationResult" - {

    "must serialise and deserialise to and from a CoreRegistrationValidationResult" - {

      "with all optional fields present" in {

        val coreRegistrationValidationResult: CoreRegistrationValidationResult =
          CoreRegistrationValidationResult(
            "IM2344433220",
            Some("IN4747493822"),
            "FR",
            true,
            Seq(Match(
              TraderId("IM0987654321"),
              Some("444444444"),
              "DE",
              Some(3),
              Some(LocalDate.now().toString),
              Some(LocalDate.now().toString),
              Some(1),
              Some(2)
            ))
          )

        val expectedJson = Json.obj(
          "searchId" -> "IM2344433220",
          "searchIdIntermediary" -> "IN4747493822",
          "searchIdIssuedBy" -> "FR",
          "traderFound" -> true,
          "matches" -> Json.arr(
            Json.obj(
              "traderId" -> "IM0987654321",
              "intermediary" -> "444444444",
              "memberState" -> "DE",
              "exclusionStatusCode" -> 3,
              "exclusionDecisionDate" -> s"${LocalDate.now()}",
              "exclusionEffectiveDate" -> s"${LocalDate.now()}",
              "nonCompliantReturns" -> 1,
              "nonCompliantPayments" -> 2
            ))
        )

        Json.toJson(coreRegistrationValidationResult) mustEqual expectedJson
        expectedJson.validate[CoreRegistrationValidationResult] mustEqual JsSuccess(coreRegistrationValidationResult)
      }

      "with all optional fields missing" in {
        val coreRegistrationValidationResult: CoreRegistrationValidationResult =
          CoreRegistrationValidationResult(
            "IM2344433220",
            None,
            "FR",
            true,
            Seq(Match(
              TraderId("IM0987654321"),
              None,
              "DE",
              None,
              None,
              None,
              None,
              None
            ))
          )

        val expectedJson = Json.obj(
          "searchId" -> "IM2344433220",
          "searchIdIssuedBy" -> "FR",
          "traderFound" -> true,
          "matches" -> Json.arr(
            Json.obj(
              "traderId" -> "IM0987654321",
              "memberState" -> "DE"
            ))
        )

        Json.toJson(coreRegistrationValidationResult) mustEqual expectedJson
        expectedJson.validate[CoreRegistrationValidationResult] mustEqual JsSuccess(coreRegistrationValidationResult)
      }

    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "searchId" -> 123456789,
        "searchIdIntermediary" -> "IN4747493822",
        "searchIdIssuedBy" -> "FR",
        "traderFound" -> true,
        "matches" -> Json.arr(
          Json.obj(
            "traderId" -> "IM0987654321",
            "intermediary" -> "444444444",
            "memberState" -> "DE",
            "exclusionStatusCode" -> 3,
            "exclusionDecisionDate" -> s"${LocalDate.now()}",
            "exclusionEffectiveDate" -> s"${LocalDate.now()}",
            "nonCompliantReturns" -> 1,
            "nonCompliantPayments" -> 2
          ))
      )

      expectedJson.validate[CoreRegistrationValidationResult] mustBe a[JsError]
    }
  }

  "Match" - {
    val activeMatch = Match(
      traderId = TraderId("IM4423268206"),
      intermediary = None,
      memberState = "HU",
      exclusionStatusCode = None,
      exclusionDecisionDate = None,
      exclusionEffectiveDate = None,
      nonCompliantReturns = None,
      nonCompliantPayments = None
    )

    val quarantinedMatch = Match(
      traderId = TraderId("IM4423268206"),
      intermediary = None,
      memberState = "HU",
      exclusionStatusCode = Some(4),
      exclusionDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusDays(2).toString),
      exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusDays(2).toString),
      nonCompliantReturns = None,
      nonCompliantPayments = None
    )

    "isActiveTrader" - {

      "must return true for active match types" in {
        activeMatch.isActiveTrader mustBe true
        quarantinedMatch.isActiveTrader mustBe false
      }

      "must return false" - {

        "for not active status codes" in {
          val nonActiveStatusCodes = Seq(1, 2, 3, 4, 5, 6)

          for (nonActiveStatusCode <- nonActiveStatusCodes) {

            val nonActiveMatch = activeMatch.copy(exclusionStatusCode = Some(nonActiveStatusCode))

            nonActiveMatch.isActiveTrader mustBe false
          }
        }
      }
    }

    "isQuarantinedTrader" - {

      "must return true for quarantined match types" in {
        quarantinedMatch.isQuarantinedTrader(stubClockAtArbitraryDate) mustBe true
        quarantinedMatch.isActiveTrader mustBe false
      }

      "must return false for a quarantined that's less than two years" in {
        val twoYearsAgo = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)
        quarantinedMatch.copy(exclusionEffectiveDate = Some(twoYearsAgo.toString)).isQuarantinedTrader(stubClockAtArbitraryDate) mustBe false
      }

      "must return false for non intermediary quarantined status codes" - {
        val statusCodes = Seq(-1, 1, 2, 3, 5, 6)

        for (statusCode <- statusCodes) {
          s"for status code $statusCode" in {
            quarantinedMatch.copy(exclusionStatusCode = Some(statusCode)).isQuarantinedTrader(stubClockAtArbitraryDate) mustBe false
          }
        }
      }
    }
  }

  "TraderIdScheme" - {
    "apply should" - {
      "return ImportOneStopShopIntermediary with intermediary number" in {
        val id = TraderId("IN9001234567")
        TraderIdScheme(id) mustBe TraderIdScheme.ImportOneStopShopIntermediary
      }

      "return ImportOneStopShopNetp with NETP number" in {
        val id = TraderId("IM9001234567")
        TraderIdScheme(id) mustBe TraderIdScheme.ImportOneStopShopNetp
      }

      "return OneStopShop with other numbers" in {
        val id = TraderId("123456789")
        TraderIdScheme(id) mustBe TraderIdScheme.OneStopShop
      }
    }
  }

}
