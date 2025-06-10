package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class PreviousSchemeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PreviousScheme" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PreviousScheme.values.toSeq)

      forAll(gen) {
        previousScheme =>

          JsString(previousScheme.toString).validate[PreviousScheme].asOpt.value mustEqual previousScheme
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PreviousScheme.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PreviousScheme] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PreviousScheme.values.toSeq)

      forAll(gen) {
        previousScheme =>

          Json.toJson(previousScheme) mustEqual JsString(previousScheme.toString)
      }
    }
  }
}
