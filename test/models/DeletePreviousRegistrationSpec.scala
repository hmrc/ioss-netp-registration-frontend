package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class DeletePreviousRegistrationSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "DeletePreviousRegistration" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(DeletePreviousRegistration.values.toSeq)

      forAll(gen) {
        deletePreviousRegistration =>

          JsString(deletePreviousRegistration.toString).validate[DeletePreviousRegistration].asOpt.value mustEqual deletePreviousRegistration
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!DeletePreviousRegistration.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[DeletePreviousRegistration] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(DeletePreviousRegistration.values.toSeq)

      forAll(gen) {
        deletePreviousRegistration =>

          Json.toJson(deletePreviousRegistration) mustEqual JsString(deletePreviousRegistration.toString)
      }
    }
  }
}
