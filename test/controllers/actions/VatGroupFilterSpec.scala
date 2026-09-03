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
import models.UserAnswers
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class VatGroupFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness() extends VatGroupFilter()(ExecutionContext.Implicits.global) {
    def callFilter(request: DataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".VatGroupFilterSpec" - {

    "should return None and allow the customer to progress when vat group is false" in {

      val dataRequest = DataRequest(FakeRequest(), "id", basicUserAnswersWithVatInfo, intermediaryNumber, None, None)

      val action = new Harness()

      val result = action.callFilter(dataRequest).futureValue

      result mustBe None

    }

    "should redirect to not authorised when vat group is true" in {
      val vatGroupVatCustomerInfo: VatCustomerInfo =
        VatCustomerInfo(
          registrationDate = LocalDate.now(stubClockAtArbitraryDate),
          desAddress = arbitraryDesAddress.arbitrary.sample.value,
          organisationName = Some("Company name"),
          individualName = None,
          singleMarketIndicator = true,
          deregistrationDecisionDate = None,
          partOfVatGroup = true,
        )

      val userAnswers = basicUserAnswersWithVatInfo.copy(vatInfo = Some(vatGroupVatCustomerInfo))

      val dataRequest = DataRequest(FakeRequest(), "id", userAnswers, intermediaryNumber, None, None)

      val action = new Harness()

      val result = action.callFilter(dataRequest).futureValue

      result mustBe Some(Redirect(controllers.routes.AccessDeniedController.onPageLoad()))
    }
  }
}
