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

package services

import base.SpecBase
import models.domain.PreviousSchemeNumbers
import models.etmp.*
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.vatEuDetails.{RegistrationType, TradingNameAndBusinessAddress}
import models.{BusinessContactDetails, ClientBusinessName, Country, Index, InternationalAddress, PreviousScheme, TradingName, UserAnswers, Website}
import pages.previousRegistrations.{PreviousEuCountryPage, PreviousOssNumberPage, PreviousSchemePage, PreviouslyRegisteredPage}
import pages.tradingNames.TradingNamePage
import pages.vatEuDetails.{EuCountryPage, EuTaxReferencePage, HasFixedEstablishmentPage, RegistrationTypePage, TradingNameAndBusinessAddressPage}
import pages.website.WebsitePage
import pages.{BusinessContactDetailsPage, ClientBusinessAddressPage, ClientBusinessNamePage, ClientCountryBasedPage, ClientTaxReferencePage}

import testutils.WireMockHelper



class AmendAnswersComparisonServiceSpec extends SpecBase with WireMockHelper {

  private val service = new AmendAnswersComparisonService()
  private val country = Country("FR", "France")
  private val changedCountry = Country("DE", "Germany")

  private val originalRegistration: EtmpDisplayRegistration = {
    registrationWrapper.etmpDisplayRegistration.copy(
      customerIdentification =
        registrationWrapper.etmpDisplayRegistration.customerIdentification.copy(
          idValue = "tax-ref-1"
        ),
      otherAddress = Some(
        EtmpOtherAddress(
          issuedBy = country.code,
          tradingName = Some("Business name"),
          addressLine1 = "Line 1",
          addressLine2 = Some("Line 2"),
          townOrCity = "Town",
          regionOrState = Some("Region"),
          postcode = Some("AA1 1AA")
        )
      ),
      tradingNames = Seq.empty,
      schemeDetails =
        registrationWrapper.etmpDisplayRegistration.schemeDetails.copy(
          previousEURegistrationDetails = Seq.empty,
          euRegistrationDetails = Seq.empty,
          websites = Seq(EtmpWebsite("www.website.com")),
          contactName = "fullName",
          businessTelephoneNumber = "555999111",
          businessEmailId = "test@test.com"
        )
    )
  }

  private val unchangedAnswers: UserAnswers = {
    emptyUserAnswers
      .set(ClientCountryBasedPage, country).success.value
      .set(ClientTaxReferencePage, "tax-ref-1").success.value
      .set(ClientBusinessNamePage, ClientBusinessName("Business name")).success.value
      .set(
        ClientBusinessAddressPage,
        InternationalAddress(
          line1 = "Line 1",
          line2 = Some("Line 2"),
          townOrCity = "Town",
          stateOrRegion = Some("Region"),
          postCode = Some("AA1 1AA"),
          country = Some(country)
        )
      ).success.value
      .set(BusinessContactDetailsPage, BusinessContactDetails("fullName", "555999111", "test@test.com")).success.value
      .set(WebsitePage(Index(0)), Website("www.website.com")).success.value
  }


  ".answersHaveChanged" - {

    "return false when nothing has changed" in {
      val result = service.answersHaveChanged(originalRegistration, unchangedAnswers)

      result mustBe false
    }

    "return true when countryBasedInChanged has changed" in {
      val answers = unchangedAnswers
        .set(ClientCountryBasedPage, changedCountry).success.value

      val result = service.answersHaveChanged(originalRegistration, answers)

      result mustBe true
    }

    "return true when tax reference has changed" in {
      val answers = unchangedAnswers
        .set(ClientTaxReferencePage, "different-tax-ref").success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when client business name has changed" in {
      val answers = unchangedAnswers
        .set(ClientBusinessNamePage, ClientBusinessName("Different name")).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when client business address has changed" in {
      val answers = unchangedAnswers
        .set(
          ClientBusinessAddressPage,
          InternationalAddress(
            line1 = "Different line 1",
            line2 = Some("Line 2"),
            townOrCity = "Town",
            stateOrRegion = Some("Region"),
            postCode = Some("AA1 1AA"),
            country = Some(country)
          )
        ).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when trading names have changed" in {
      val answers = unchangedAnswers
        .set(TradingNamePage(Index(0)), TradingName("New trading name")).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when previous registrations have changed" in {
      val answers = unchangedAnswers
        .set(PreviouslyRegisteredPage, true).success.value
        .set(PreviousEuCountryPage(Index(0)), changedCountry).success.value
        .set(PreviousSchemePage(Index(0), Index(0)), PreviousScheme.OSSU).success.value
        .set(PreviousOssNumberPage(Index(0), Index(0)), PreviousSchemeNumbers("foo")).success.value
      
      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when fixed establishments have changed" in {
      val answers = unchangedAnswers
        .set(HasFixedEstablishmentPage, true).success.value
        .set(EuCountryPage(Index(0)), changedCountry).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when website has changed" in {
      val answers = unchangedAnswers
        .set(WebsitePage(Index(0)), Website("www.different-website.com")).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when contact details have changed" in {
      val answers = unchangedAnswers
        .set(
          BusinessContactDetailsPage,
          BusinessContactDetails(
            fullName = "Different Name",
            telephoneNumber = "555999111",
            emailAddress = "test@test.com"
          )
        ).success.value

      service.answersHaveChanged(originalRegistration, answers) mustBe true
    }

    "return true when fixed establishment details have changed" in {
      val euCountry = Country("FR", "France")

      val original = originalRegistration.copy(
        schemeDetails = originalRegistration.schemeDetails.copy(
          euRegistrationDetails = Seq(
            EtmpDisplayEuRegistrationDetails(
              issuedBy = euCountry.code,
              vatNumber = Some("123456789"),
              taxIdentificationNumber = Some("tax-ref"),
              fixedEstablishmentTradingName = "FE name",
              fixedEstablishmentAddressLine1 = "Line 1",
              fixedEstablishmentAddressLine2 = Some("Line 2"),
              townOrCity = "Town",
              regionOrState = Some("Region"),
              postcode = Some("AA1 1AA")
            )
          )
        )
      )

      val answers = unchangedAnswers
        .set(HasFixedEstablishmentPage, true).success.value
        .set(EuCountryPage(Index(0)), euCountry).success.value
        .set(RegistrationTypePage(Index(0)), RegistrationType.TaxId).success.value
        .set(EuTaxReferencePage(Index(0)), "tax-ref").success.value
        .set(TradingNameAndBusinessAddressPage(Index(0)), TradingNameAndBusinessAddress(
            tradingName = TradingName("Different FE name"),
            address = InternationalAddress(
              line1 = "Line 1",
              line2 = Some("Line 2"),
              townOrCity = "Town",
              stateOrRegion = Some("Region"),
              postCode = Some("AA1 1AA"),
              country = Some(euCountry)
            )
          )
        ).success.value

      service.answersHaveChanged(original, answers) mustBe true
    }

    "return true when previous registration scheme details have changed" in {
      val previousCountry = Country("FR", "France")

      val original =
        originalRegistration.copy(
          schemeDetails = originalRegistration.schemeDetails.copy(
            previousEURegistrationDetails = Seq(
              EtmpPreviousEuRegistrationDetails(
                issuedBy = previousCountry.code,
                registrationNumber = "FR123456789",
                schemeType = SchemeType.OSSUnion,
                intermediaryNumber = None
              )
            )
          )
        )

      val answers = unchangedAnswers
        .set(PreviouslyRegisteredPage, true).success.value
        .set(PreviousEuCountryPage(Index(0)), changedCountry).success.value

      service.answersHaveChanged(original, answers) mustBe true
    }
  }
}