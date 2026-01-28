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
import config.FrontendAppConfig
import formats.Format.dateFormatter
import models.{ActiveTraderResult, ClientBusinessName, UserAnswers}
import pages.ClientBusinessNamePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.ActiveTraderResultQuery
import views.html.ClientAlreadyRegisteredView

import java.time.LocalDate

class ClientAlreadyRegisteredControllerSpec extends SpecBase {

  private val clientBusinessName: ClientBusinessName = ClientBusinessName(vatCustomerInfo.organisationName.value)
  private val exclusionEffectiveDate: String = arbitraryEtmpExclusion.arbitrary.sample.value.effectiveDate.toString

  private val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
    isReversal = false,
    exclusionEffectiveDate = Some(exclusionEffectiveDate)
  )

  private val userAnswers = emptyUserAnswersWithVatInfo
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(ActiveTraderResultQuery, activeTraderResult).success.value

  "ClientAlreadyRegistered Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ClientAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientAlreadyRegisteredView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        val formattedExclusionEffectiveDate: String = LocalDate.parse(exclusionEffectiveDate).format(dateFormatter)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          clientBusinessName.name,
          Some(formattedExclusionEffectiveDate),
          config.intermediaryYourAccountUrl,
          isReversal = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is no retrievable company name information" in {

      val userAnswersWithoutCompanyName: UserAnswers = userAnswers
        .remove(ClientBusinessNamePage).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithoutCompanyName)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ClientAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientAlreadyRegisteredView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        val formattedExclusionEffectiveDate: String = LocalDate.parse(exclusionEffectiveDate).format(dateFormatter)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          clientBusinessName.name,
          Some(formattedExclusionEffectiveDate),
          config.intermediaryYourAccountUrl,
          isReversal = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is no exclusion effective date" in {

      val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
        isReversal = false,
        exclusionEffectiveDate = None
      )

      val updatedAnswers = userAnswers
        .set(ActiveTraderResultQuery, activeTraderResult).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ClientAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientAlreadyRegisteredView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          clientBusinessName.name,
          None,
          config.intermediaryYourAccountUrl,
          isReversal = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is an exclusion effective date but exclusion status code is -1" in {

      val activeTraderResult: ActiveTraderResult = ActiveTraderResult(
        isReversal = true,
        exclusionEffectiveDate = Some(exclusionEffectiveDate)
      )

      val updatedAnswers = userAnswers
        .set(ActiveTraderResultQuery, activeTraderResult).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ClientAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientAlreadyRegisteredView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        val formattedExclusionEffectiveDate: String = LocalDate.parse(exclusionEffectiveDate).format(dateFormatter)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          clientBusinessName.name,
          Some(formattedExclusionEffectiveDate),
          config.intermediaryYourAccountUrl, isReversal = true)(request, messages(application)).toString
      }
    }

    "must throw an Exception when Active Trader Result isn't set" in {

      val errorMessage: String = "No active trader result retrieved"

      val updatedAnswers = userAnswers
        .remove(ActiveTraderResultQuery).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ClientAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }
      }
    }
  }
}
