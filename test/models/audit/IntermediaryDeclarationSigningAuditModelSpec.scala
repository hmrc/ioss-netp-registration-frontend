package models.audit

import base.SpecBase
import models.requests.DataRequest
import play.api.libs.json.{JsValue, Json}
import testutils.RegistrationData.emptyUserAnswers

class IntermediaryDeclarationSigningAuditModelSpec extends SpecBase {

  private val submissionResult: SubmissionResult = SubmissionResult.values.head

  "RegistrationAuditModelSpec" - {

    "must create correct json object" in {

      implicit val dataRequest: DataRequest[_] =
        DataRequest(fakeRequest, userAnswersId, emptyUserAnswers)

      val intermediaryDeclarationSigningAuditModel = IntermediaryDeclarationSigningAuditModel.build(
        intermediaryDeclarationSigningAuditType = IntermediaryDeclarationSigningAuditType.CreateDeclaration,
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

      intermediaryDeclarationSigningAuditModel.detail mustBe expectedJson
    }
  }
}
