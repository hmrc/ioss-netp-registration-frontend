package models.previousRegistrations

import models.PreviousEuCountry
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class PreviousEuCountrySpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PreviousEuCountry" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PreviousEuCountry.values.toSeq)

      forAll(gen) {
        previousEuCountry =>

          JsString(previousEuCountry.toString).validate[PreviousEuCountry].asOpt.value mustEqual previousEuCountry
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PreviousEuCountry.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PreviousEuCountry] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PreviousEuCountry.values.toSeq)

      forAll(gen) {
        previousEuCountry =>

          Json.toJson(previousEuCountry) mustEqual JsString(previousEuCountry.toString)
      }
    }
  }
}
