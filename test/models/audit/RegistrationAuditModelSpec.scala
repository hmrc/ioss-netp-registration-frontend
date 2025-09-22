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
import models.requests.ClientOptionalDataRequest
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

class RegistrationAuditModelSpec extends SpecBase {

  private val submissionResult: SubmissionResult = SubmissionResult.Success
  private val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")
  private val testUserId = "test-user-id-12345"

  "RegistrationAuditModelSpec" - {

    "must create correct json object with enrollment response" in {

      val fakeRequest = FakeRequest("POST", "/declaration")
        .withHeaders("user-agent" -> "test-browser/1.0")

      implicit val dataRequest: ClientOptionalDataRequest[AnyContent] = ClientOptionalDataRequest(
        fakeRequest,
        testUserId,
        emptyUserAnswers
      )

      val registrationAuditModel = RegistrationAuditModel.build(
        userAnswers = emptyUserAnswers,
        etmpEnrolmentResponse = Some(etmpEnrolmentResponse),
        submissionResult = submissionResult
      )

      val expectedJson = Json.obj(
        "credId" -> testUserId,
        "browserUserAgent" -> "test-browser/1.0",
        "userAnswersDetails" -> Json.toJson(emptyUserAnswers),
        "etmpEnrolmentResponse" -> Json.toJson(etmpEnrolmentResponse),
        "submissionResult" -> submissionResult.toString
      )

      registrationAuditModel.detail mustBe expectedJson
      registrationAuditModel.transactionName mustBe "netp-registration-submitted"
    }

    "must create correct json object without enrollment response" in {

      val fakeRequest = FakeRequest("POST", "/declaration")

      implicit val dataRequest: ClientOptionalDataRequest[AnyContent] = ClientOptionalDataRequest(
        fakeRequest,
        testUserId,
        emptyUserAnswers
      )

      val registrationAuditModel = RegistrationAuditModel.build(
        userAnswers = emptyUserAnswers,
        etmpEnrolmentResponse = None,
        submissionResult = SubmissionResult.Failure
      )

      val expectedJson = Json.obj(
        "credId" -> testUserId,
        "browserUserAgent" -> "",
        "userAnswersDetails" -> Json.toJson(emptyUserAnswers),
        "etmpEnrolmentResponse" -> Json.toJson(None: Option[EtmpEnrolmentResponse]),
        "submissionResult" -> SubmissionResult.Failure.toString
      )

      registrationAuditModel.detail mustBe expectedJson
      registrationAuditModel.transactionName mustBe "netp-registration-submitted"
    }

    "must handle missing user agent" in {

      val fakeRequest = FakeRequest("POST", "/declaration")

      implicit val dataRequest: ClientOptionalDataRequest[AnyContent] = ClientOptionalDataRequest(
        fakeRequest,
        testUserId,
        emptyUserAnswers
      )

      val registrationAuditModel = RegistrationAuditModel.build(
        userAnswers = emptyUserAnswers,
        etmpEnrolmentResponse = Some(etmpEnrolmentResponse),
        submissionResult = submissionResult
      )

      val jsonDetail = registrationAuditModel.detail
      (jsonDetail \ "browserUserAgent").as[String] mustBe ""
    }
  }
}