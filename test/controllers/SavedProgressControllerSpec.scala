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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import connectors.SaveForLaterConnector
import formats.Format.saveForLaterDateFormatter
import models.responses.InternalServerError
import models.{SavedUserAnswers, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, SavedProgressPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.{AuthenticatedUserAnswersRepository, SessionRepository}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import utils.FutureSyntax.FutureOps
import views.html.SavedProgressView

import java.time.temporal.ChronoUnit

class SavedProgressControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  private val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")
  private val s4lTtl: Int = 28

  private lazy val saveForLaterRoute: String = routes.SavedProgressController.onPageLoad(waypoints, continueUrl).url

  private val answersExpiryDate: String = emptyUserAnswers.lastUpdated.plus(s4lTtl, ChronoUnit.DAYS)
    .atZone(stubClockAtArbitraryDate.getZone).toLocalDate.format(saveForLaterDateFormatter)

  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterConnector)
    Mockito.reset(mockSessionRepository)
  }

  "SavedProgress Controller" - {

    "must save the user answers and return OK and the correct view for a GET when connector returns Right(Some(savedUserAnswers))" in {

      val answers: UserAnswers = emptyUserAnswersWithVatInfo.set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(Some(savedUserAnswers)).toFuture
        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val request = FakeRequest(GET, saveForLaterRoute)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        val view = application.injector.instanceOf[SavedProgressView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(answersExpiryDate, continueUrl.get(OnlyRelative).url, config.loginUrl)(request, messages(application)).toString
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to Journey Recovery when connector returns Right(None)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(None).toFuture

        val request = FakeRequest(GET, saveForLaterRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must redirect to Journey Recovery when connector returns Left((error)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Left(InternalServerError).toFuture

        val request = FakeRequest(GET, saveForLaterRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verifyNoInteractions(mockSessionRepository)
      }
    }
  }
}
