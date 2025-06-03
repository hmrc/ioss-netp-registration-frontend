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
import models.UserAnswers
import models.requests.OptionalDataRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientDeclarationPage
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest

import scala.concurrent.{ExecutionContext, Future}

class ClientDeclarationFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness() extends ClientDeclarationFilter()(ExecutionContext.Implicits.global) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".ClientDeclarationFilter" - {

    "should return None and allow the customer to progress" - {

      "when the client Declaration page has been filled" in {

        val userAnswers = UserAnswers("id").set(ClientDeclarationPage, true).success.value

        val optionalDataRequest = OptionalDataRequest(FakeRequest(), "id", Some(userAnswers), None)

        val action = new Harness()

        val result = action.callFilter(optionalDataRequest).futureValue

        result mustBe None

      }
    }

    "should redirect to journey recovery" - {

      "when the client declaration page is false" in {
        val userAnswers = UserAnswers("id").set(ClientDeclarationPage, false).success.value

        val optionalDataRequest = OptionalDataRequest(FakeRequest(), "id", Some(userAnswers), None)

        val action = new Harness()

        val result = action.callFilter(optionalDataRequest).futureValue

        result mustBe Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad()))
      }

      "when the client declaration page has NOT been filled" in {
        val userAnswers = UserAnswers("id")

        val optionalDataRequest = OptionalDataRequest(FakeRequest(), "id", Some(userAnswers), None)

        val action = new Harness()

        val result = action.callFilter(optionalDataRequest).futureValue

        result mustBe Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad()))
      }

      "when the userAnswers are not present to continue the journey" in {

        val optionalDataRequest = OptionalDataRequest(FakeRequest(), "id", None, None)

        val action = new Harness()

        val result = action.callFilter(optionalDataRequest).futureValue

        result mustBe Some(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyRecoveryController.onPageLoad()))
      }
    }
  }
}



