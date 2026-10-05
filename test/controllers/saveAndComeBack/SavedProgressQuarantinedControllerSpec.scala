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

package controllers.saveAndComeBack

import base.SpecBase
import config.FrontendAppConfig
import formats.Format.dateFormatter
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.SavedProgressQuarantinedView

import java.time.LocalDate

class SavedProgressQuarantinedControllerSpec extends SpecBase with MockitoSugar {

  private val quarantinedEffectiveDate: String = LocalDate.now(stubClockAtArbitraryDate).toString

  private val quarantinedExpirationDate: String = LocalDate
    .parse(quarantinedEffectiveDate)
    .plusYears(2)
    .format(dateFormatter)

  "SavedProgressQuarantined Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSessionRepository: SessionRepository = mock[SessionRepository]

      when(mockSessionRepository.clear(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SavedProgressQuarantinedController.onPageLoad(quarantinedEffectiveDate).url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        val view = application.injector.instanceOf[SavedProgressQuarantinedView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(quarantinedExpirationDate, config.intermediaryYourAccountUrl)(request, messages(application)).toString
        verify(mockSessionRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }
}