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
import models.requests.{ClientOptionalDataRequest, OptionalDataRequest}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientDataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(sessionRepository: SessionRepository) extends ClientDataRetrievalActionImpl(sessionRepository) {
    def callTransform[A](request: OptionalDataRequest[A]): Future[ClientOptionalDataRequest[A]] = transform(request)
  }

  "Client Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must error when userAnswers are 'None' in the request" in {

        val userAnswers: Option[UserAnswers] = None

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(userAnswers)
        val action = new Harness(sessionRepository)

        val result: Throwable = action.callTransform(OptionalDataRequest(FakeRequest(), "id", userAnswers, Some(intermediaryNumber))).failed.futureValue

        result mustBe a[IllegalStateException]
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        val userAnswers = UserAnswers("id")

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(Some(userAnswers))
        val action = new Harness(sessionRepository)

        val result = action.callTransform(OptionalDataRequest(FakeRequest(), "id", Some(userAnswers), Some(intermediaryNumber))).futureValue

        result.userAnswers mustBe userAnswers
      }
    }
  }
}
