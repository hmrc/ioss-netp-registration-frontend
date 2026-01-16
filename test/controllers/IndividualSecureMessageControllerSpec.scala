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
import connectors.SecureMessageConnector
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import views.html.secureMessages.IndividualSecureMessageView

import scala.concurrent.Future

class IndividualSecureMessageControllerSpec extends SpecBase {

  val mockConnector: SecureMessageConnector = mock[SecureMessageConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val messageId = "123"
  val message = "Hello World"

  "IndividualSecureMessage Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockConnector.getMessage(eqTo(messageId))(any()))
        .thenReturn(Future.successful(Right(message)))

      when(mockConnector.markAsRead(eqTo(messageId))(any()))
        .thenReturn(Future.successful(Right(())))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SecureMessageConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.secureMessages.routes.IndividualSecureMessageController.onPageLoad(messageId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndividualSecureMessageView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(messageId, message)(request, messages(application)).toString
      }
    }
  }
}