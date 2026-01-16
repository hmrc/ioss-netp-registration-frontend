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

package connectors.test

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import models.responses.UnexpectedResponseStatus
import models.testOnly.TestOnlyCodes
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, UNPROCESSABLE_ENTITY, UNSUPPORTED_MEDIA_TYPE}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier


class TestOnlyClientDeclarationCodeConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val otherErrorStatuses: Seq[Int] = Seq(BAD_REQUEST, UNSUPPORTED_MEDIA_TYPE, UNPROCESSABLE_ENTITY)

  private def application: Application = applicationBuilder()
    .configure(
      "microservice.services.ioss-netp-registration.port" -> server.port(),
    )
    .build()

  ".getTestOnlyCode" - {

    val uniqueUrlCode: String = "ABCDEF"
    val uniqueActivationCode: TestOnlyCodes = TestOnlyCodes(uniqueUrlCode, "GHIJKL")

    val url: String = s"/ioss-netp-registration/test-only/ActivationCode/$uniqueUrlCode"

    "must return a Right(uniqueActivationCode) for a given urlCode when the server returns an existing one" in {

      val responseBody = Json.toJson(uniqueActivationCode).toString

      running(application) {

        val connector = application.injector.instanceOf[TestOnlyClientDeclarationCodeConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(ok()
              .withBody(responseBody))
        )

        val result = connector.getTestOnlyCode(uniqueUrlCode).futureValue

        result `mustBe` Right(uniqueActivationCode)
      }
    }

    otherErrorStatuses.foreach { status =>
      s"must return Left(UnexpectedResponseStatus) when the server returns status: $status" in {

        val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to retrieve test registration code, status $status returned")

        running(application) {

          val connector = application.injector.instanceOf[TestOnlyClientDeclarationCodeConnector]

          server.stubFor(
            WireMock.get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status))
          )

          val result = connector.getTestOnlyCode(uniqueUrlCode).futureValue

          result `mustBe` Left(response)
        }
      }
    }
  }

}
