package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class RegistrationTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "RegistrationType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(RegistrationType.values.toSeq)

      forAll(gen) {
        registrationType =>

          JsString(registrationType.toString).validate[RegistrationType].asOpt.value mustEqual registrationType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!RegistrationType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[RegistrationType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(RegistrationType.values.toSeq)

      forAll(gen) {
        registrationType =>

          Json.toJson(registrationType) mustEqual JsString(registrationType.toString)
      }
    }
  }
}
