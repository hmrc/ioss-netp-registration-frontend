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

package models.audit

import base.SpecBase
import models.core.{CoreRegistrationRequest, CoreRegistrationValidationResult, Match, TraderId}
import models.requests.DataRequest
import play.api.libs.json.{JsValue, Json}
import testutils.RegistrationData.emptyUserAnswers

import java.time.LocalDate

class CoreRegistrationAuditModelSpec extends SpecBase {

  private val coreRegistrationRequest: CoreRegistrationRequest =
    arbitraryCoreRegistrationRequest.arbitrary.sample.value
  private val coreRegistrationValidationResult: CoreRegistrationValidationResult =
    arbitraryCoreRegistrationValidationResult.arbitrary.sample.value

  val coreRegistrationRequestDetail: JsValue = Json.obj(
    "pointOfSubmission" -> coreRegistrationRequest.source,
    "scheme" -> coreRegistrationRequest.scheme,
    "validationSearchId" -> coreRegistrationRequest.searchId,
    "searchIntermediary" -> coreRegistrationRequest.searchIntermediary,
    "countryCodeSearchIdIssuedBy" -> coreRegistrationRequest.searchIdIssuedBy
  )

  "CoreRegistrationAuditModelSpec" - {

    "must create correct json object when trader is found" in {

      implicit val dataRequest: DataRequest[_] =
        DataRequest(fakeRequest, userAnswersId, emptyUserAnswers, intermediaryDetails.intermediaryNumber, None)

      val coreRegistrationValidationResultWithTrader =  coreRegistrationValidationResult.copy(
        traderFound = true,
        matches = Seq(Match(
          TraderId("IM0987654321"),
          Some("444444444"),
          "DE",
          Some(3),
          Some(LocalDate.now().toString),
          Some(LocalDate.now().toString),
          Some(1),
          Some(2)))
      )

      val coreRegistrationValidationResultDetail: JsValue = Json.obj(
        "validationSearchId" -> coreRegistrationValidationResultWithTrader.searchId,
        "searchIdIntermediary" -> coreRegistrationValidationResultWithTrader.searchIdIntermediary,
        "countryCodeSearchIdIssuedBy" -> coreRegistrationValidationResultWithTrader.searchIdIssuedBy,
        "traderFound" -> coreRegistrationValidationResultWithTrader.traderFound,
        "matches" -> coreRegistrationValidationResultWithTrader.matches
      )

      val coreRegistrationAuditModel = CoreRegistrationAuditModel.build(
        coreRegistrationRequest = coreRegistrationRequest,
        coreRegistrationValidationResult = coreRegistrationValidationResultWithTrader
      )


      val expectedJson: JsValue = Json.obj(
        "credId" -> dataRequest.userId,
        "browserUserAgent" -> "",
        "requestersIntermediaryNumber" -> Json.toJson(dataRequest.intermediaryNumber),
        "coreRegistrationRequest" -> Json.toJson(coreRegistrationRequestDetail),
        "coreRegistrationValidationResponse" -> Json.toJson(coreRegistrationValidationResultDetail)
      )

      coreRegistrationAuditModel.detail `mustBe` expectedJson
    }

    "must create correct json object when trader is not found" in {

      implicit val dataRequest: DataRequest[_] =
        DataRequest(fakeRequest, userAnswersId, emptyUserAnswers, intermediaryDetails.intermediaryNumber, None)

      val coreRegistrationValidationResultWithoutTrader = coreRegistrationValidationResult.copy(traderFound = false)

      val coreRegistrationValidationResultDetail: JsValue = Json.obj(
        "validationSearchId" -> coreRegistrationValidationResultWithoutTrader.searchId,
        "searchIdIntermediary" -> coreRegistrationValidationResultWithoutTrader.searchIdIntermediary,
        "countryCodeSearchIdIssuedBy" -> coreRegistrationValidationResultWithoutTrader.searchIdIssuedBy,
        "traderFound" -> coreRegistrationValidationResultWithoutTrader.traderFound,
      )

      val coreRegistrationAuditModel = CoreRegistrationAuditModel.build(
        coreRegistrationRequest = coreRegistrationRequest,
        coreRegistrationValidationResult = coreRegistrationValidationResultWithoutTrader
      )


      val expectedJson: JsValue = Json.obj(
        "credId" -> dataRequest.userId,
        "browserUserAgent" -> "",
        "requestersIntermediaryNumber" -> Json.toJson(dataRequest.intermediaryNumber),
        "coreRegistrationRequest" -> Json.toJson(coreRegistrationRequestDetail),
        "coreRegistrationValidationResponse" -> Json.toJson(coreRegistrationValidationResultDetail)
      )

      coreRegistrationAuditModel.detail `mustBe` expectedJson
    }
  }
}
