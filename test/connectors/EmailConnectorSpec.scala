/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import config.Constants
import models.emails.EmailSendingResult.{EMAIL_ACCEPTED, EMAIL_NOT_SENT, EMAIL_UNSENDABLE}
import models.emails.{ClientDeclarationEmailParameters, EmailSendingResult, EmailToSendRequest}
import play.api.Application
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class EmailConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val request = EmailToSendRequest(
    to = List("name@example.com"),
    templateId = Constants.clientDeclarationEmailTemplateId,
    parameters = ClientDeclarationEmailParameters(
      intermediary_name = "Hello",
      recipientName_line1 = "These",
      activation_code = "Are",
      activation_code_expiry_date = "Strings")
  )

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.email.port" -> server.port)
      .build()

  "EmailConnector.send" - {
    "should return EMAIL_ACCEPTED when status >= 200 and < 300" in {
      running(application) {

        val connector = application.injector.instanceOf[EmailConnector]

        server.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .willReturn(aResponse.withStatus(200))
        )

        connector.send(request).futureValue mustBe EMAIL_ACCEPTED
      }
    }

    "should return EMAIL_UNSENDABLE when status >= 400 and < 500" in {
      running(application) {
        val connector = application.injector.instanceOf[EmailConnector]

        server.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .willReturn(aResponse.withStatus(400))
        )

        connector.send(request).futureValue mustBe EMAIL_UNSENDABLE
      }
    }

    "should return EMAIL_NOT_SENT when status >= 500 and < 600" in {
      running(application) {
        val connector = application.injector.instanceOf[EmailConnector]

        server.stubFor(
          post(urlEqualTo("/hmrc/email"))
            .willReturn(aResponse.withStatus(500))
        )

        connector.send(request).futureValue mustBe EMAIL_NOT_SENT
      }
    }
  }
}
