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

package utils

import base.SpecBase
import models.requests.DataRequest
import models.*
import models.vatEuDetails.RegistrationType.VatNumber
import models.vatEuDetails.{EuDetails, TradingNameAndBusinessAddress}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.vatEuDetails.*
import pages.website.WebsitePage
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.*

class CompletionChecksSpec extends SpecBase with MockitoSugar {

  private object CompletionChecksTests extends CompletionChecks

  private val clientBusinessName: ClientBusinessName = ClientBusinessName(vatCustomerInfo.organisationName.value)
  private val countries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(countries).sample.value
  private val tradingNameIndex: Index = Index(0)
  private val tradingName: TradingName = arbitraryTradingName.arbitrary.sample.value
  private val businessAddress: InternationalAddress = InternationalAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    stateOrRegion = None,
    postCode = None,
    country = Some(country)
  )

  private val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value
    .copy(registrationType = Some(VatNumber))

  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(BusinessBasedInUKPage, true).success.value
    .set(ClientHasVatNumberPage, true).success.value
    .set(ClientVatNumberPage, vatNumber).success.value
    .set(ClientHasUtrNumberPage, true).success.value
    .set(ClientUtrNumberPage, utr).success.value
    .set(ClientsNinoNumberPage, nino).success.value
    .set(ClientTaxReferencePage, taxReference).success.value
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(ClientBusinessAddressPage, businessAddress).success.value
    .set(HasTradingNamePage, true).success.value
    .set(TradingNamePage(tradingNameIndex), tradingName).success.value
    .set(PreviouslyRegisteredPage, false).success.value
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), euDetails.euCountry).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), euDetails.tradingNameAndBusinessAddress.value).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euDetails.euVatNumber.value).success.value
    .set(WebsitePage(Index(0)), Website("www.test-website.com")).success.value
    .set(BusinessContactDetailsPage, BusinessContactDetails("fullName", "555999111", "test@test.com")).success.value


  "CompletionChecks" - {

    ".validate" - {

      "must validate and return true when valid data is present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]

          when(request.userAnswers) thenReturn validAnswers

          val result = CompletionChecksTests.validate()

          result `mustBe` true
        }
      }

      "must validate and return false when invalid data is present" in {

        val invalidAnswers: UserAnswers = validAnswers
          .remove(WebsitePage(Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]

          when(request.userAnswers) thenReturn invalidAnswers

          val result = CompletionChecksTests.validate()

          result `mustBe` false
        }
      }

      "must validate and return false when contact details are missing" in {

        val invalidAnswers = validAnswers.remove(BusinessContactDetailsPage).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]

          when(request.userAnswers) thenReturn invalidAnswers

          val result = CompletionChecksTests.validate()

          result `mustBe` false
        }
      }
    }

    ".getFirstValidationErrorRedirect" - {

      "must obtain the first validation error and redirect to the correct page" - {

        "when there is only one validation error present" in {

          val invalidAnswers: UserAnswers = validAnswers
            .remove(WebsitePage(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints)

            result `mustBe` Some(Redirect(WebsitePage(Index(0)).route(waypoints).url))
          }
        }

        "when there are multiple validation errors present" in {

          val invalidAnswers: UserAnswers = validAnswers
            .remove(TradingNamePage(Index(0))).success.value
            .remove(WebsitePage(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints)

            result `mustBe` Some(Redirect(TradingNamePage(Index(0)).route(waypoints).url))
          }
        }
      }

      "must return None when there are no validation errors present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints)

          result `mustBe` None
        }
      }
    }
  }
}
