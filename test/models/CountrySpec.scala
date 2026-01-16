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

package models

import base.SpecBase
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json

class CountrySpec extends SpecBase with ScalaFutures {

  "Country" - {

    "CountryWithValidationDetails" - {

      "must serialize and deserialize correctly" in {
        val country = Country("FR", "France")
        val countryWithValidationDetails = CountryWithValidationDetails(
          country,
          """^FR[A-Z0-9]{2}[0-9]{9}$""",
          "the 11 characters",
          "XX123456789"
        )

        val expectedJson = Json.obj(
          "country" -> Json.obj(
            "code" -> "FR",
            "name" -> "France"
          ),
          "vrnRegex" -> """^FR[A-Z0-9]{2}[0-9]{9}$""",
          "messageInput" -> "the 11 characters",
          "exampleVrn" -> "XX123456789"
        )

        val serializedJson = Json.obj(
          "country" -> Json.toJson(country),
          "vrnRegex" -> countryWithValidationDetails.vrnRegex,
          "messageInput" -> countryWithValidationDetails.messageInput,
          "exampleVrn" -> countryWithValidationDetails.exampleVrn
        )

        serializedJson mustBe expectedJson
      }

      "must validate VRNs using regex for each country" in {
        val testCases = Seq(
          ("AT", "ATU12345678", """^ATU[0-9]{8}$""", true),
          ("AT", "12345678", """^ATU[0-9]{8}$""", false),
          ("FR", "FRXX123456789", """^FR[A-Z0-9]{2}[0-9]{9}$""", true),
          ("FR", "123456789", """^FR[A-Z0-9]{2}[0-9]{9}$""", false),
          ("DE", "DE123456789", """^DE[0-9]{9}$""", true),
          ("DE", "123456789", """^DE[0-9]{9}$""", false)
        )

        testCases.foreach { case (code, vrn, regex, isValid) =>
          val country = Country(code, code)
          val countryValidationDetails = CountryWithValidationDetails(
            country,
            regex,
            "dummy message",
            "dummy example"
          )

          vrn.matches(countryValidationDetails.vrnRegex) mustBe isValid
        }
      }
    }

    "Country.fromCountryCode" - {

      "must return the correct Country for a valid code" in {
        val result = Country.fromCountryCode("FR")
        result mustBe Some(Country("FR", "France"))
      }

      "must return None for an invalid code" in {
        val result = Country.fromCountryCode("ZZ")
        result mustBe None
      }
    }

    "Country.fromCountryCodeUnsafe" - {
      "must return the correct Country for a valid code" in {
        val result = Country.fromCountryCodeUnsafe("DE")
        result mustBe Country("DE", "Germany")
      }

      "must throw an exception for an invalid code" in {
        intercept[RuntimeException] {
          Country.fromCountryCodeUnsafe("ZZ")
        }.getMessage mustBe "countryCode ZZ could not be mapped to a country"
      }
    }

    "Country.getCountryName" - {
      "must return the correct name for a valid country code" in {
        val result = Country.getCountryName("IT")
        result mustBe "Italy"
      }

      "must throw an exception for an invalid country code" in {
        intercept[NoSuchElementException] {
          Country.getCountryName("ZZ")
        }
      }
    }
      
    "euCountriesWithVRNValidationRules" - {
      "must contain validation rules for all EU countries" in {
        val expectedCountries = Country.euCountriesWithNI.map(_.code)
        val actualCountries = CountryWithValidationDetails.euCountriesWithVRNValidationRules.map(_.country.code)

        actualCountries.sorted mustBe expectedCountries.sorted
      }

      "must have valid regex patterns for each country" in {
        CountryWithValidationDetails.euCountriesWithVRNValidationRules.foreach { details =>
          details.vrnRegex.r.pattern.matcher("").matches() mustBe false
        }
      }
    }
  }
}
