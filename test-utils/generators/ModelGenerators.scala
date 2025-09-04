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

import models.*
import models.core.{CoreRegistrationRequest, CoreRegistrationValidationResult}
import models.domain.ModelHelpers.normaliseSpaces
import models.domain.VatCustomerInfo
import models.etmp.SchemeType
import models.iossRegistration.*
import models.ossRegistration.*
import models.vatEuDetails.{EuDetails, RegistrationType, TradingNameAndBusinessAddress}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{choose, listOfN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.EitherValues
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.util.UUID

trait ModelGenerators extends EitherValues with EtmpModelGenerators {

  implicit lazy val arbitraryRegistrationType: Arbitrary[RegistrationType] = {
    Arbitrary {
      Gen.oneOf(RegistrationType.values)
    }
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
      Gen.oneOf(Country.euCountries)
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

  implicit lazy val arbitraryWebsite: Arbitrary[Website] =
    Arbitrary {
      for {
        site <- Gen.alphaStr
      } yield Website(site)
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
        uniqueUrlCode = UUID.randomUUID().toString
        uniqueActivationCode = UUID.randomUUID().toString
      } yield {
        SavedPendingRegistration(
          journeyId = userAnswers.journeyId,
          uniqueUrlCode = uniqueUrlCode,
          userAnswers = userAnswers,
          lastUpdated = userAnswers.lastUpdated,
          uniqueActivationCode = uniqueActivationCode,
          intermediaryDetails = IntermediaryDetails("IM123456789", "IntermediaryName"))
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

  implicit lazy val arbitraryEuTaxReference: Gen[String] = {
    Gen.listOfN(maxEuTaxReferenceLength, Gen.alphaNumChar).map(_.mkString)
  }

  implicit lazy val genEuTaxReference: Gen[String] = {
    Gen.listOfN(maxEuTaxReferenceLength, Gen.alphaNumChar).map(_.mkString)
  }

  implicit lazy val arbitraryTradingNameAndBusinessAddress: Arbitrary[TradingNameAndBusinessAddress] =
    Arbitrary {
      for {
        name <- arbitrary[TradingName]
        addr <- arbitrary[InternationalAddress]
      } yield TradingNameAndBusinessAddress(name, addr)
    }

  implicit lazy val arbitraryEuDetails: Arbitrary[EuDetails] = {
    Arbitrary {
      for {
        hasFixedEstablishment <- arbitrary[Boolean]
        registrationType <- arbitraryRegistrationType.arbitrary
        euTaxReference <- genEuTaxReference
        euVatNumber = arbitraryEuVatNumber.sample.get
        countryCode = euVatNumber.substring(0, 2)
        euCountry = Country.euCountries.find(_.code == countryCode).head
        tradingNameAndBusinessAddress <- arbitrary[TradingNameAndBusinessAddress]
      } yield EuDetails(
        euCountry = euCountry,
        hasFixedEstablishment = Some(hasFixedEstablishment),
        registrationType = Some(registrationType),
        euVatNumber = Some(euVatNumber),
        euTaxReference = Some(euTaxReference),
        tradingNameAndBusinessAddress = Some(tradingNameAndBusinessAddress),
      )
    }
  }

  implicit lazy val arbitraryBankDetails: Arbitrary[BankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield BankDetails(accountName, bic, iban)
    }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9).map(_.toString.head))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9).map(_.toString.head)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }

  implicit lazy val arbitraryIban: Arbitrary[Iban] = {
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).toOption.get)
    }
  }

  implicit val arbitraryIossEtmpExclusion: Arbitrary[IossEtmpExclusion] = {
    Arbitrary {
      for {
        exclusionReason <- Gen.oneOf(IossEtmpExclusionReason.values)
        effectiveDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31))
        decisionDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31))
        quarantine <- arbitrary[Boolean]
      } yield IossEtmpExclusion(
        exclusionReason = exclusionReason,
        effectiveDate = effectiveDate,
        decisionDate = decisionDate,
        quarantine = quarantine
      )
    }
  }

  implicit lazy val arbitraryIossEtmpTradingName: Arbitrary[IossEtmpTradingName] = {
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield IossEtmpTradingName(tradingName)
    }
  }

  implicit lazy val arbitraryIossEtmpDisplayRegistration: Arbitrary[IossEtmpDisplayRegistration] = {
    Arbitrary {
      for {
        tradingNames <- Gen.listOfN(3, arbitraryIossEtmpTradingName.arbitrary)
        schemeDetails <- arbitraryIossEtmpDisplaySchemeDetails.arbitrary
        bankDetails <- arbitraryIossEtmpBankDetails.arbitrary
        exclusions <- arbitraryIossEtmpExclusion.arbitrary
      } yield IossEtmpDisplayRegistration(
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails,
        exclusions = Seq(exclusions)
      )
    }
  }

  implicit lazy val arbitraryIossEtmpDisplaySchemeDetails: Arbitrary[IossEtmpDisplaySchemeDetails] = {
    Arbitrary {
      for {
        contactName <- Gen.alphaStr
        businessTelephoneNumber <- Gen.alphaStr
        businessEmailId <- Gen.alphaStr
      } yield IossEtmpDisplaySchemeDetails(
        contactName = contactName,
        businessTelephoneNumber = businessTelephoneNumber,
        businessEmailId = businessEmailId
      )
    }
  }

  implicit lazy val arbitraryIossEtmpBankDetails: Arbitrary[IossEtmpBankDetails] = {
    Arbitrary {
      for {
        accountName <- Gen.alphaStr
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield IossEtmpBankDetails(accountName, bic, iban)
    }
  }

  implicit lazy val arbitrarySalesChannels: Arbitrary[SalesChannels] = {
    Arbitrary {
      Gen.oneOf(SalesChannels.values)
    }
  }

  implicit lazy val arbitraryFixedEstablishment: Arbitrary[OssTradeDetails] = {
    Arbitrary {
      for {
        tradingName <- arbitrary[String]
        address <- arbitrary[InternationalAddress]
      } yield OssTradeDetails(tradingName, address)
    }
  }

  implicit val arbitraryEuTaxIdentifierType: Arbitrary[OssEuTaxIdentifierType] = {
    Arbitrary {
      Gen.oneOf(OssEuTaxIdentifierType.values)
    }
  }

  implicit val arbitraryEuTaxIdentifier: Arbitrary[OssEuTaxIdentifier] = {
    Arbitrary {
      for {
        identifierType <- arbitrary[OssEuTaxIdentifierType]
        value <- arbitrary[Int].map(_.toString)
      } yield OssEuTaxIdentifier(identifierType, value)
    }
  }

  implicit val arbitraryOssExcludedTrader: Arbitrary[OssExcludedTrader] = {
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
        exclusionReason <- Gen.oneOf(ExclusionReason.values)
        effectiveDate <- arbitraryDate.arbitrary
        quarantined <- arbitrary[Boolean]
      } yield OssExcludedTrader(
        vrn = vrn,
        exclusionReason = Some(exclusionReason),
        effectiveDate = Some(effectiveDate),
        quarantined = Some(quarantined)
      )
    }
  }

  implicit lazy val arbitraryVrn: Arbitrary[Vrn] = {
    Arbitrary {
      for {
        chars <- Gen.listOfN(9, Gen.numChar)
      } yield Vrn(chars.mkString(""))
    }
  }

  implicit val arbitraryOssRegistration: Arbitrary[OssRegistration] = {
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
        name <- arbitrary[String]
        vatDetails <- arbitrary[OssVatDetails]
        contactDetails <- arbitrary[OssContactDetails]
        bankDetails <- arbitrary[BankDetails]
        commencementDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        isOnlineMarketplace <- arbitrary[Boolean]
        adminUse <- arbitrary[OssAdminUse]
      } yield OssRegistration(vrn, name, Nil, vatDetails, Nil, contactDetails, Nil, commencementDate, Nil, bankDetails, isOnlineMarketplace, None, None, None, None, None, None, None, None, adminUse)
    }
  }

  implicit lazy val arbitraryOssVatDetails: Arbitrary[OssVatDetails] = {
    Arbitrary {
      for {
        registrationDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        address <- arbitrary[Address]
        partOfVatGroup <- arbitrary[Boolean]
        source <- arbitrary[OssVatDetailSource]
      } yield OssVatDetails(registrationDate, address, partOfVatGroup, source)
    }
  }

  implicit lazy val arbitraryOssBusinessContactDetails: Arbitrary[OssContactDetails] = {
    Arbitrary {
      for {
        fullName <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress <- arbitrary[String]
      } yield OssContactDetails(fullName, telephoneNumber, emailAddress)
    }
  }

  implicit lazy val arbitraryAdminUse: Arbitrary[OssAdminUse] = {
    Arbitrary {
      for {
        changeDate <- Gen.const(LocalDateTime.now())
      } yield OssAdminUse(Some(changeDate))
    }
  }

  implicit val arbitraryAddress: Arbitrary[Address] = {
    Arbitrary {
      Gen.oneOf(
        arbitrary[InternationalAddress],
        arbitrary[DesAddress]
      )
    }
  }

  implicit val arbitraryOssVatDetailSource: Arbitrary[OssVatDetailSource] = {
    Arbitrary(
      Gen.oneOf(OssVatDetailSource.values)
    )
  }

  implicit val arbitraryIntermediaryDetails: Arbitrary[IntermediaryDetails] = {
    Arbitrary {
      for {
        intermediaryNumber <- Gen.alphaNumStr
        intermediaryName <- Gen.alphaStr
      } yield IntermediaryDetails(
        intermediaryNumber,
        intermediaryName
      )
    }
  }

  implicit val arbitrarySaveForLaterRequest: Arbitrary[SaveForLaterRequest] = {
    Arbitrary {
      for {
        intermediaryNumber <- Gen.alphaNumStr
        journeyId = UUID.randomUUID().toString
        data = Json.toJson("savedAnswers")
      } yield {
        SaveForLaterRequest(
          journeyId = journeyId,
          data = data,
          intermediaryNumber = intermediaryNumber
        )
      }
    }
  }

  implicit val arbitrarySavedUserAnswers: Arbitrary[SavedUserAnswers] = {
    Arbitrary {
      val intermediaryNumber = arbitrarySaveForLaterRequest.arbitrary.sample.get.intermediaryNumber
      val journeyId = arbitrarySaveForLaterRequest.arbitrary.sample.get.journeyId
      val data = JsObject(Seq("savedUserAnswers" -> Json.toJson("userAnswers")))
      val now = Instant.now

      SavedUserAnswers(
        journeyId = journeyId,
        data = data,
        intermediaryNumber = intermediaryNumber,
        lastUpdated = now
      )
    }
  }

  implicit val arbitraryCoreRegistrationRequest: Arbitrary[CoreRegistrationRequest] = {
    Arbitrary {
      for {
        scheme <- Gen.alphaStr
        source <- Gen.alphaStr
        searchId <- Gen.alphaStr
        searchIntermediary <- Gen.alphaStr
        searchIdIssuedBy <- Gen.alphaStr
      } yield {
        CoreRegistrationRequest(
          scheme = Some(scheme),
          source = source,
          searchId = searchId,
          searchIntermediary = Some(searchIntermediary),
          searchIdIssuedBy = searchIdIssuedBy
        )
      }
    }
  }

  implicit val arbitraryCoreRegistrationValidationResult: Arbitrary[CoreRegistrationValidationResult] = {
    Arbitrary {
      for {
        searchId <- Gen.alphaStr
        searchIntermediary <- Gen.alphaStr
        searchIdIssuedBy <- Gen.alphaStr
        traderFound <- arbitrary[Boolean]
        matches = Seq.empty
      } yield {
        CoreRegistrationValidationResult(
          searchId = searchId,
          searchIntermediary = Some(searchIntermediary),
          searchIdIssuedBy = searchIdIssuedBy,
          traderFound = traderFound,
          matches = matches
        )
      }
    }
  }
}
