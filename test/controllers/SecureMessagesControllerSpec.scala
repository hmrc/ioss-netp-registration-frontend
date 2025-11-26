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
import connectors.{RegistrationConnector, SecureMessageConnector}
import models.responses.InternalServerError
import models.securemessage.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps

class SecureMessagesControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockSecureMessageConnector: SecureMessageConnector = mock[SecureMessageConnector]
  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  val emptyTaxpayerName = TaxpayerName(
    title = None,
    forename = None,
    secondForename = None,
    surname = None,
    honours = None,
    line1 = None,
    line2 = None,
    line3 = None
  )

  val testSecureMessageResponse = SecureMessageResponse(
    messageType = "messageType",
    id = "secureId",
    subject = "subject",
    issueDate = "2025-10-06",
    senderName = "senderName",
    unreadMessages = true,
    count = 3,
    taxpayerName = Some(emptyTaxpayerName),
    validFrom = "2025-10-06",
    sentInError = false,
    language = Some("en")
  )

  val testSecureMessageCount = SecureMessageCount(total = 3, unread = 3)

  val secureMessageResponseWithCount = SecureMessageResponseWithCount(
    items = Seq(testSecureMessageResponse),
    count = testSecureMessageCount
  )

  override def beforeEach(): Unit = reset(mockSecureMessageConnector)

  "SecureMessagesController" - {

    "must return OK and the correct for a GET" in {

      when(mockSecureMessageConnector.getMessages(any(), any(), any(), any(), any())(any()))
        .thenReturn(Right(secureMessageResponseWithCount).toFuture)

      when(mockRegistrationConnector.displayRegistrationNetp(any())(any()))
        .thenReturn(Right(registrationWrapper).toFuture)

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(
          bind[SecureMessageConnector].toInstance(mockSecureMessageConnector),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.secureMessages.routes.SecureMessagesController.onPageLoad(waypoints).url)

        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(mockSecureMessageConnector, times(1)).getMessages(any(), any(), any(), any(), any())(any())
      }
    }

    "must throw an exception when the connector fails to return secure messages" in {

      when(mockSecureMessageConnector.getMessages(any(), any(), any(), any(), any())(any()))
        .thenReturn(Left(InternalServerError).toFuture)

      when(mockRegistrationConnector.displayRegistrationNetp(any())(any()))
        .thenReturn(Right(registrationWrapper).toFuture)

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .overrides(
          bind[SecureMessageConnector].toInstance(mockSecureMessageConnector),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.secureMessages.routes.SecureMessagesController.onPageLoad(waypoints).url)

        assertThrows[Exception] {
          route(application, request).value.futureValue
        }

        verify(mockSecureMessageConnector, times(1)).getMessages(any(), any(), any(), any(), any())(any())
      }
    }
  }
}

