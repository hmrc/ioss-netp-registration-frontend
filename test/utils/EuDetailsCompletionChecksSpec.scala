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
import models.vatEuDetails.TradingNameAndBusinessAddress
import models.vatEuDetails.RegistrationType.VatNumber
import models.requests.DataRequest
import models.{Country, InternationalAddress, TradingName, UserAnswers}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.vatEuDetails.*
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.running

class EuDetailsCompletionChecksSpec extends SpecBase with MockitoSugar {

  private val euDetailsCompletionChecksTest: EuDetailsCompletionChecks.type = EuDetailsCompletionChecks

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress = TradingNameAndBusinessAddress(
    tradingName = TradingName("Company name"),
    address = InternationalAddress(
      line1 = "line-1",
      line2 = None,
      townOrCity = "town-or-city",
      stateOrRegion = None,
      postCode = None,
      country = Some(country)
    )
  )



  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(0)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value
    .set(AddEuDetailsPage(Some(countryIndex(0))), true).success.value
    .set(EuCountryPage(countryIndex(1)), country).success.value
    .set(TradingNameAndBusinessAddressPage(countryIndex(1)), tradingNameAndBusinessAddress).success.value
    .set(RegistrationTypePage(countryIndex(1)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(1)), euVatNumber).success.value


  "EuDetailsCompletionChecks" - {

    ".isEuDetailsDefined" - {

      "when the HasFixedEstablishmentPage question is Yes" - {

        "must return true when answers for the section are defined" in {

          val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

          running(application) {

            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
            when(request.userAnswers) thenReturn validAnswers

            val result = euDetailsCompletionChecksTest.isEuDetailsDefined()

            result mustBe true
          }
        }

        "must return false when answers for the section are absent" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

          running(application) {

            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
            when(request.userAnswers) thenReturn validAnswers

            val result = euDetailsCompletionChecksTest.isEuDetailsDefined()

            result mustBe true
          }
        }
      }

      "when the HasFixedEstablishmentPage question is No" - {

        "must return true when answers for the section are empty" in {

          val emptyAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(HasFixedEstablishmentPage, false).success.value

          val application = applicationBuilder(userAnswers = Some(emptyAnswers)).build()

          running(application) {

            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
            when(request.userAnswers) thenReturn emptyAnswers

            val result = euDetailsCompletionChecksTest.isEuDetailsDefined()

            result mustBe true
          }
        }

        "must return false when answers for the section are defined" in {

          val answers: UserAnswers = validAnswers
            .set(HasFixedEstablishmentPage, false).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {

            implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
            when(request.userAnswers) thenReturn answers

            val result = euDetailsCompletionChecksTest.isEuDetailsDefined()

            result mustBe false
          }
        }
      }
    }

    ".emptyEuDetailsRedirect" - {

      "must return Some(redirect) when EU details are not defined" in {

        val userAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn userAnswers

          val result = euDetailsCompletionChecksTest.emptyEuDetailsRedirect(waypoints)

          result mustBe Some(Redirect(HasFixedEstablishmentPage.route(waypoints).url))
        }

      }

      "must return None when EU details are defined" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTest.emptyEuDetailsRedirect(waypoints)

          result mustBe None
        }
      }
    }

    ".getIncompleteEuDetails" - {

      "must return Some(EuDetails) if required fields are missing or invalid" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .set(EuVatNumberPage(countryIndex(0)), "").success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTest.getIncompleteEuDetails(countryIndex(0))

          result mustBe defined
        }
      }

      "must return None if details are complete and valid" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTest.emptyEuDetailsRedirect(waypoints)

          result mustBe None
        }
      }
    }

    ".getAllIncompleteEuDetails" - {

      "must return a list of incomplete details" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .set(EuVatNumberPage(countryIndex(0)), "INVALIDVAT").success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTest.getAllIncompleteEuDetails()

          result.length mustBe 1
        }
      }

      "must return empty list if all details are valid" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTest.getAllIncompleteEuDetails()

          result mustBe empty
        }
      }
    }

    ".incompleteEuDetailsRedirect" - {

      "must return Some(redirect) to the first incomplete field" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
          .remove(EuVatNumberPage(countryIndex(0))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTest.incompleteEuDetailsRedirect(waypoints)

          result mustBe defined
        }

      }

      "must return None if all details are complete" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTest.incompleteEuDetailsRedirect(waypoints)

          result mustBe None
        }
      }
    }
  }
}
