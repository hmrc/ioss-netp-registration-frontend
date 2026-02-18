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

package controllers

import base.SpecBase
import formats.Format.dateFormatter
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.OtherCountryExcludedAndQuarantinedView

import java.time.LocalDate

class OtherCountryExcludedAndQuarantinedControllerSpec extends SpecBase with MockitoSugar {

  "OtherCountryExcludedAndQuarantined Controller" - {

    "must delete the answers and return OK and the correct view for a GET" in {

      val countryCode: String = "NL"
      val countryName: String = "Netherlands"
      val effectiveDecisionDate: String = "2022-10-10"
      val formattedEffectiveDecisionDate: String = LocalDate.parse(effectiveDecisionDate).plusYears(2).format(dateFormatter)

      val mockSessionRepository: SessionRepository = mock[SessionRepository]

      when(mockSessionRepository.clear(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(countryCode, effectiveDecisionDate).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OtherCountryExcludedAndQuarantinedView]

        status(result) `mustBe` OK
        contentAsString(result) mustBe view(countryName, formattedEffectiveDecisionDate)(request, messages(application)).toString
        verify(mockSessionRepository, times(1)).clear(eqTo(userAnswersId))
      }
    }
  }
}
