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

package pages.previousRegistrations

import base.SpecBase
import models.domain.PreviousSchemeNumbers
import models.{Country, Index, PreviousScheme}
import org.scalatest.matchers.should.Matchers.shouldBe

class PreviousEuCountryPageSpec extends SpecBase {

  private val index: Index = Index(0)
  private val country = Country.euCountries.head
  private val page = PreviousEuCountryPage(index)

  ".cleanup" - {

    "should remove previously entered OSS number, IOSS number, and scheme details when a new country is selected" in {

      val userAnswers = basicUserAnswersWithVatInfo
        .set(PreviousOssNumberPage(index, index), PreviousSchemeNumbers("ATU1234567")).success.value
        .set(PreviousSchemePage(index, index), PreviousScheme.OSSU).success.value
        .set(PreviousIossNumberPage(index, index), PreviousSchemeNumbers("IN1001234567")).success.value

      val result = page.cleanup(Some(country), userAnswers).get

      result.get(PreviousOssNumberPage(index, index)) shouldBe  None
      result.get(PreviousSchemePage(index, index)) shouldBe None
      result.get(PreviousIossNumberPage(index, index)) shouldBe None
    }

    "should not modify UserAnswers when no country is selected (None)" in {

      val result = page.cleanup(None, basicUserAnswersWithVatInfo).get

      result shouldBe basicUserAnswersWithVatInfo
    }
  }
}
