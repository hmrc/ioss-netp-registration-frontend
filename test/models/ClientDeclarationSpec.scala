package models

import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class ClientDeclarationSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues with ModelGenerators {

  "ClientDeclaration" - {

    "must deserialise valid values" in {

      val gen = arbitrary[ClientDeclaration]

      forAll(gen) {
        clientDeclaration =>

          JsString(clientDeclaration.toString).validate[ClientDeclaration].asOpt.value mustEqual clientDeclaration
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!ClientDeclaration.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[ClientDeclaration] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = arbitrary[ClientDeclaration]

      forAll(gen) {
        clientDeclaration =>

          Json.toJson(clientDeclaration) mustEqual JsString(clientDeclaration.toString)
      }
    }
  }
}
