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

package generators

import models.domain.ModelHelpers.normaliseSpaces
import models.domain.VatCustomerInfo
import models.*
import models.etmp.SchemeType
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{choose, listOfN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.EitherValues
import play.api.libs.json.{JsObject, Json}

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID

trait ModelGenerators extends EitherValues {

  implicit lazy val arbitraryRegistrationType: Arbitrary[RegistrationType] =
    Arbitrary {
      Gen.oneOf(RegistrationType.values.toSeq)
    }

  private val maxFieldLength: Int = 35

  private val maxEuTaxReferenceLength: Int = 20

  private def commonFieldString(maxLength: Int): Gen[String] = (for {
    length <- choose(1, maxLength)
    chars <- listOfN(length, commonFieldSafeInputs)
  } yield chars.mkString).retryUntil(_.trim.nonEmpty)

  private def commonFieldSafeInputs: Gen[Char] = Gen.oneOf(
    Gen.alphaNumChar,
    Gen.oneOf('À' to 'ÿ'),
    Gen.const('.'),
    Gen.const(','),
    Gen.const('/'),
    Gen.const('’'),
    Gen.const('\''),
    Gen.const('"'),
    Gen.const('_'),
    Gen.const('&'),
    Gen.const(' '),
    Gen.const('\'')
  )

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        line3 <- Gen.option(commonFieldString(maxFieldLength))
        line4 <- Gen.option(commonFieldString(maxFieldLength))
        line5 <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries.map(_.code))
      } yield DesAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(line3),
        normaliseSpaces(line4),
        normaliseSpaces(line5),
        normaliseSpaces(postCode),
        country
      )
    }

  implicit lazy val arbitraryInternationalAddress: Arbitrary[InternationalAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        townOrCity <- commonFieldString(maxFieldLength)
        stateOrRegion <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries)
      } yield InternationalAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(townOrCity),
        normaliseSpaces(stateOrRegion),
        normaliseSpaces(postCode),
        Some(country)
      )
    }

  implicit lazy val arbitraryTradingName: Arbitrary[TradingName] = {
    Arbitrary {
      for {
        name <- commonFieldString(maxFieldLength)
      } yield {
        TradingName(name)
      }
    }
  }

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.allCountries)
    }

  implicit val arbitraryVatCustomerInfo: Arbitrary[VatCustomerInfo] = {
    Arbitrary {
      for {
        desAddress <- arbitraryDesAddress.arbitrary
        registrationDate <- arbitraryDate.arbitrary
        organisationName <- Gen.alphaStr
        individualName <- Gen.alphaStr
        singleMarketIndicator <- arbitrary[Boolean]
      } yield {
        VatCustomerInfo(
          desAddress = desAddress,
          registrationDate = registrationDate,
          organisationName = Some(organisationName),
          individualName = Some(individualName),
          singleMarketIndicator = singleMarketIndicator,
          deregistrationDecisionDate = None
        )
      }
    }
  }

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  implicit lazy val arbitraryDate: Arbitrary[LocalDate] = {
    Arbitrary {
      datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2023, 12, 31))
    }
  }

  implicit lazy val arbitraryBusinessContactDetails: Arbitrary[BusinessContactDetails] = {
    Arbitrary {
      for {
        fullName <- Gen.alphaStr.retryUntil(fn => fn.length > 1 && fn.length < 99)
        telephoneNumber <- Gen.numStr.retryUntil(tn => tn.length > 1 && tn.length < 19)
        userName = Gen.alphaStr.retryUntil(un => un.length > 1 && un.length < 22).sample.head
        hostName = Gen.alphaStr.retryUntil(hn => hn.length > 1 && hn.length < 22).sample.head
        domain = Gen.oneOf(Seq(".com", ".co.uk", ".org")).sample.head
      } yield {
        BusinessContactDetails(
          fullName = fullName,
          telephoneNumber = telephoneNumber,
          emailAddress = s"$userName@$hostName$domain"
        )
      }
    }
  }

  implicit lazy val arbitraryClientBusinessName: Arbitrary[ClientBusinessName] = {
    Arbitrary {
      for {
        clientBusinessName <- Gen.alphaStr.retryUntil(cbn => cbn.length > 1 && cbn.length < 40)
      } yield {
        ClientBusinessName(name = clientBusinessName)
      }
    }
  }

  implicit lazy val arbitraryUserAnswers: Arbitrary[UserAnswers] = {
    Arbitrary {
      for {
        id <- arbitrary[String]
        journeyId = UUID.randomUUID().toString
        data = JsObject(Seq("test" -> Json.toJson("test")))
        vatInfo <- arbitraryVatCustomerInfo.arbitrary
        lastUpdated = Instant.now().truncatedTo(ChronoUnit.MILLIS)
      } yield {
        UserAnswers(
          id = id,
          journeyId = journeyId,
          data = data,
          vatInfo = Some(vatInfo),
          lastUpdated = lastUpdated
        )
      }
    }
  }

  implicit lazy val arbitrarySavedPendingRegistration: Arbitrary[SavedPendingRegistration] = {
    Arbitrary {
      for {
        userAnswers <- arbitraryUserAnswers.arbitrary
        uniqueCode = UUID.randomUUID().toString
      } yield {
        SavedPendingRegistration(
          journeyId = userAnswers.journeyId,
          uniqueCode = uniqueCode,
          userAnswers = userAnswers,
          lastUpdated = userAnswers.lastUpdated
        )
      }
    }
  }

  implicit lazy val arbitrarySchemeType: Arbitrary[SchemeType] =
    Arbitrary {
      Gen.oneOf(SchemeType.values)
    }

  implicit lazy val arbitraryPreviousScheme: Arbitrary[PreviousScheme] =
    Arbitrary {
      Gen.oneOf(PreviousScheme.values)
    }

  implicit lazy val arbitraryPreviousSchemeType: Arbitrary[PreviousSchemeType] =
    Arbitrary {
      Gen.oneOf(PreviousSchemeType.values)
    }

  implicit lazy val arbitraryEuVatNumber: Gen[String] = {
    for {
      countryCode <- Gen.oneOf(Country.euCountries.map(_.code))
      matchedCountryRule = CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == countryCode).head
    } yield s"$countryCode${matchedCountryRule.exampleVrn}"
  }

  implicit lazy val genEuTaxReference: Gen[String] = {
    Gen.listOfN(maxEuTaxReferenceLength, Gen.alphaNumChar).map(_.mkString)
  }
}
