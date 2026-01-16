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

package controllers.actions

import base.SpecBase
import models.Index
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import pages.*
import pages.previousRegistrations.*
import pages.tradingNames.*
import pages.vatEuDetails.*
import pages.website.*

import scala.concurrent.ExecutionContext.Implicits.global

class CheckAmendPageAccessFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val filter = new CheckAmendPageAccessFilter()

  ".shouldBlockPage" - {

    "when checking BusinessBasedInUKPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage, false) mustBe true
      }

      "must return true for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage, false) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage, false) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage, false) mustBe true
      }
    }

    "when checking ClientHasVatNumberPage" - {

      "must always return true regardless of client type" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasVatNumberPage, false) mustBe true
      }
    }

    "when checking ClientVatNumberPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage, false) mustBe true
      }

      "must return true for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage, false) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage, false) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage, false) mustBe true
      }
    }

    "when checking ClientHasUtrNumberPage" - {

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage, false) mustBe true
      }

      "must return true for UK-based client with NINO" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage, false) mustBe true
      }

      "must return true for non-UK client" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage, false) mustBe true
      }
    }

    "when checking ClientUtrNumberPage" - {

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientUtrNumberPage, false) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientUtrNumberPage, false) mustBe true
      }
    }

    "when checking ClientsNinoNumberPage" - {

      "must return true for UK-based client with NINO" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage, false) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage, false) mustBe true
      }

      "must return true for non-UK client" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage, false) mustBe true
      }
    }

    "when checking ClientBusinessAddressPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage, false) mustBe true
      }

      "must return false for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage, false) mustBe false
      }

      "must return false for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage, false) mustBe false
      }
    }

    "when checking ClientCountryBasedPage" - {

      "must return true for all client types" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientCountryBasedPage, false) mustBe true
      }
    }

    "when checking CheckVatDetailsPage" - {

      "must always return true regardless of client type" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), CheckVatDetailsPage(), false) mustBe true
      }
    }

    "when UserAnswers is None" - {

      "must return false (normal registration)" in {
        filter.shouldBlockPage(None, BusinessBasedInUKPage, false) mustBe false
      }
    }

    "when there is an exclusion" - {

      "must block all pages except business contact details" in {
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), BusinessBasedInUKPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientHasVatNumberPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), CheckVatDetailsPage(), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientHasUtrNumberPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientUtrNumberPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientsNinoNumberPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientVatNumberPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientBusinessAddressPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientCountryBasedPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientTaxReferencePage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientBusinessNamePage, true) mustBe true

        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), TradingNamePage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), HasTradingNamePage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), AddTradingNamePage(), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteAllTradingNamesPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteTradingNamePage(Index(0)), true) mustBe true

        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviouslyRegisteredPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviousEuCountryPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviousSchemeTypePage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), ClientHasIntermediaryPage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviousSchemePage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), CheckPreviousSchemeAnswersPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviousOssNumberPage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), PreviousIossNumberPage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), AddPreviousRegistrationPage(), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteAllPreviousRegistrationsPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeletePreviousSchemePage(Index(0), Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeletePreviousRegistrationPage(Index(0)), true) mustBe true

        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), HasFixedEstablishmentPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), EuCountryPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), TradingNameAndBusinessAddressPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), RegistrationTypePage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), EuVatNumberPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), EuTaxReferencePage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), CheckEuDetailsAnswersPage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), AddEuDetailsPage(), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteAllEuDetailsPage, true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteEuDetailsPage(Index(0)), true) mustBe true

        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), WebsitePage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), AddWebsitePage(), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), DeleteWebsitePage(Index(0)), true) mustBe true
        filter.shouldBlockPage(Some(basicUserAnswersWithVatInfo), BusinessContactDetailsPage, true) mustBe false
      }
    }
  }
}