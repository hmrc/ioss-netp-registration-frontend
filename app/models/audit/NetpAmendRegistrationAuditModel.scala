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

import models.UserAnswers
import models.etmp.amend.AmendRegistrationResponse
import models.requests.AuthenticatedMandatoryRegistrationRequest
import play.api.libs.json.{JsValue, Json}

case class NetpAmendRegistrationAuditModel(
                                            registrationAuditType: RegistrationAuditType,
                                            userId: String,
                                            userAgent: String,
                                            userAnswers: UserAnswers,
                                            amendRegistrationResponse: Option[AmendRegistrationResponse],
                                            submissionResult: SubmissionResult
                                          ) extends JsonAuditModel {

  override val auditType: String = registrationAuditType.auditType

  override val transactionName: String = registrationAuditType.transactionName

  override val detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "userAnswersDetails" -> Json.toJson(userAnswers),
    "amendRegistrationResponse" -> Json.toJson(amendRegistrationResponse),
    "submissionResult" -> submissionResult
  )
}

object NetpAmendRegistrationAuditModel {

  def build(
             registrationAuditType: RegistrationAuditType,
             userAnswers: UserAnswers,
             amendRegistrationResponse: Option[AmendRegistrationResponse],
             submissionResult: SubmissionResult
           )(implicit request: AuthenticatedMandatoryRegistrationRequest[_]): NetpAmendRegistrationAuditModel =
    NetpAmendRegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      userId = request.userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      userAnswers = userAnswers,
      amendRegistrationResponse = amendRegistrationResponse,
      submissionResult = submissionResult
    )
}