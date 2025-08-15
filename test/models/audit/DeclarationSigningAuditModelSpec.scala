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

package models.audit

import base.SpecBase
import models.requests.DataRequest
import play.api.libs.json.{JsValue, Json}
import testutils.RegistrationData.emptyUserAnswers

class DeclarationSigningAuditModelSpec extends SpecBase {

  private val submissionResult: SubmissionResult = SubmissionResult.values.head

  "RegistrationAuditModelSpec" - {

    "must create correct json object" in {

      implicit val dataRequest: DataRequest[_] =
        DataRequest(fakeRequest, userAnswersId, emptyUserAnswers, intermediaryDetails.intermediaryNumber)

      val declarationSigningAuditModel = DeclarationSigningAuditModel.build(
        declarationSigningAuditType = DeclarationSigningAuditType.CreateDeclaration,
        userAnswers = emptyUserAnswers,
        submissionResult = SubmissionResult.Success,
        submittedDeclarationPageBody = ""
      )

      val expectedJson: JsValue = Json.obj(
        "credId" -> dataRequest.userId,
        "browserUserAgent" -> "",
        "userAnswersDetails" -> Json.toJson(emptyUserAnswers),
        "submissionResult" -> submissionResult,
        "submittedDeclarationPageBody" -> ""
      )

      declarationSigningAuditModel.detail mustBe expectedJson
    }
  }
}
