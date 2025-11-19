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

package controllers.actions

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import pages._

import scala.concurrent.ExecutionContext.Implicits.global

class CheckAmendPageAccessFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val filter = new CheckAmendPageAccessFilter()

  ".shouldBlockPage" - {

    "when checking BusinessBasedInUKPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage) mustBe true
      }

      "must return true for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), BusinessBasedInUKPage) mustBe true
      }
    }

    "when checking ClientHasVatNumberPage" - {

      "must always return true regardless of client type" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasVatNumberPage) mustBe true
      }
    }

    "when checking ClientVatNumberPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage) mustBe true
      }

      "must return true for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientVatNumberPage) mustBe true
      }
    }

    "when checking ClientHasUtrNumberPage" - {

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage) mustBe true
      }

      "must return true for UK-based client with NINO" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage) mustBe true
      }

      "must return true for non-UK client" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientHasUtrNumberPage) mustBe true
      }
    }

    "when checking ClientUtrNumberPage" - {

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientUtrNumberPage) mustBe true
      }

      "must return true for non-UK client without VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientUtrNumberPage) mustBe true
      }
    }

    "when checking ClientsNinoNumberPage" - {

      "must return true for UK-based client with NINO" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage) mustBe true
      }

      "must return true for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage) mustBe true
      }

      "must return true for non-UK client" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientsNinoNumberPage) mustBe true
      }
    }

    "when checking ClientBusinessAddressPage" - {

      "must return true for UK-based client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage) mustBe true
      }

      "must return false for UK-based client with UTR" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, false).success.value
          .set(ClientHasUtrNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage) mustBe false
      }

      "must return false for non-UK client with VAT" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientBusinessAddressPage) mustBe false
      }
    }

    "when checking ClientCountryBasedPage" - {

      "must return true for all client types" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, true).success.value
          .set(ClientHasVatNumberPage, true).success.value

        filter.shouldBlockPage(Some(userAnswers), ClientCountryBasedPage) mustBe true
      }
    }

    "when checking CheckVatDetailsPage" - {

      "must always return true regardless of client type" in {
        val userAnswers = emptyUserAnswers
          .set(BusinessBasedInUKPage, false).success.value
          .set(ClientHasVatNumberPage, false).success.value

        filter.shouldBlockPage(Some(userAnswers), CheckVatDetailsPage()) mustBe true
      }
    }

    "when UserAnswers is None" - {

      "must return false (normal registration)" in {
        filter.shouldBlockPage(None, BusinessBasedInUKPage) mustBe false
      }
    }
  }
}