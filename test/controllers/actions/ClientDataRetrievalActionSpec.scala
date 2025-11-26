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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import queries.ClientUrlCodeQuery
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientDataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(sessionRepository: SessionRepository) extends ClientDataRetrievalActionImpl(sessionRepository) {
    def callRefine[A](request: OptionalDataRequest[A]): Future[Either[Result, ClientOptionalDataRequest[A]]] = refine(request)
  }

  "Client Data Retrieval Action" - {
    
    val intermediaryNumber = arbitraryIntermediaryDetails.arbitrary.sample.get.intermediaryNumber

    "when there is no data in the cache" - {

      "must direct the user to ClientJourneyStartController to re-pull data and restart the journey" in {

        val userAnswers: Option[UserAnswers] = None

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(userAnswers)

        val action = new Harness(sessionRepository)

        val result = action.callRefine(OptionalDataRequest(
          FakeRequest("GET", "UrlCode"), "id", userAnswers, Some(intermediaryNumber), Enrolments(Set.empty))).futureValue

        result mustBe Left(Redirect(controllers.clientDeclarationJourney.routes.ClientJourneyStartController.onPageLoad(EmptyWaypoints, "UrlCode")))
      }
    }

    "when there is data in the cache" - {

      "and the clientUrlCode is NOT stored, must store the code and build a ClientOptionalDataRequest" in {

        val userAnswers = UserAnswers("id")

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(Some(userAnswers))
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        val action = new Harness(sessionRepository)

        val result = action.callRefine(
          OptionalDataRequest(FakeRequest("GET", "UrlCode"), "id", Some(userAnswers), Some(intermediaryNumber), Enrolments(Set.empty))).futureValue

        result.value.userAnswers.isDefined
        result.value.userAnswers.get(ClientUrlCodeQuery).isDefined
        result.value.userAnswers.get(ClientUrlCodeQuery).value mustEqual "UrlCode"
        result.exists(_.isInstanceOf[ClientOptionalDataRequest[_]])
      }

      "and the clientUrlCode is stored, must store the code and build a ClientOptionalDataRequest" in {

        val userAnswers = UserAnswers("id").set(ClientUrlCodeQuery, "UrlCode").success.value

        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(Some(userAnswers))
        val action = new Harness(sessionRepository)

        val result = action.callRefine(
          OptionalDataRequest(FakeRequest(), "id", Some(userAnswers), Some(intermediaryNumber), Enrolments(Set.empty))).futureValue


        result.value.userAnswers.isDefined
        result.value.userAnswers.get(ClientUrlCodeQuery).isDefined
        result.value.userAnswers.get(ClientUrlCodeQuery).value mustEqual "UrlCode"
        result.exists(_.isInstanceOf[ClientOptionalDataRequest[_]])
      }
    }
  }
}
