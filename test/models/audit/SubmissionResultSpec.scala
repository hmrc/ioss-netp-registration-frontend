package models.audit

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}


class SubmissionResultSpec extends SpecBase with ScalaCheckPropertyChecks {

  "SubmissionResultS" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SubmissionResult.values)

      forAll(gen) {
        submissionResult =>

          JsString(submissionResult.toString).validate[SubmissionResult].asOpt.value mustBe submissionResult
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!SubmissionResult.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[SubmissionResult] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SubmissionResult.values)

      forAll(gen) {
        submissionResult =>

          Json.toJson(submissionResult) mustBe JsString(submissionResult.toString)
      }
    }
  }
}

