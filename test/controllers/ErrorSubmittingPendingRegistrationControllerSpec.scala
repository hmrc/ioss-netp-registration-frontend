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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ErrorSubmittingPendingRegistrationView

class ErrorSubmittingPendingRegistrationControllerSpec extends SpecBase {

  "ErrorSubmittingPendingRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ErrorSubmittingPendingRegistrationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ErrorSubmittingPendingRegistrationView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view()(request, messages(application)).toString
      }
    }
  }
}
