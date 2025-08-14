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

package controllers.clientDeclarationJourney

import base.SpecBase
import controllers.clientDeclarationJourney
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.etmp.EtmpEnrolmentResponseQuery
import views.html.clientDeclarationJourney.ClientSuccessfulRegistrationView

class ClientSuccessfulRegistrationControllerSpec extends SpecBase {

  "ClientSuccessfulRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val iossNumber = "IM9004444444"
      val userAnswers = basicUserAnswersWithVatInfo
        .set(EtmpEnrolmentResponseQuery, EtmpEnrolmentResponse(iossNumber)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clientDeclarationJourney.routes.ClientSuccessfulRegistrationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClientSuccessfulRegistrationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(iossNumber)(request, messages(application)).toString
      }
    }
  }
}
