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
import models.requests.ClientOptionalDataRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientCodeEntryPage
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest

import scala.concurrent.{ExecutionContext, Future}

class ClientValidationFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness() extends ClientValidationFilter()(ExecutionContext.Implicits.global) {
    def callFilter(request: ClientOptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".ClientValidationFilter" - {

    "should return None and allow the customer to progress when the code entry page has been filled" in {

      val userAnswers = UserAnswers("id").set(ClientCodeEntryPage("UrlCode"), "activationCode").success.value

      val clientOptionDataRequest = ClientOptionalDataRequest(FakeRequest(), "id", userAnswers)

      val action = new Harness()

      val result = action.callFilter(clientOptionDataRequest).futureValue

      result mustBe None

    }
    "should redirect to journey recovery when the code entry page has NOT been filled" in {
      val userAnswers = UserAnswers("id")

      val clientOptionDataRequest = ClientOptionalDataRequest(FakeRequest(), "id", userAnswers)

      val action = new Harness()

      val result = action.callFilter(clientOptionDataRequest).futureValue

      result mustBe Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad()))
    }
  }
}
