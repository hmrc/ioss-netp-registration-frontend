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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.responses.{ConflictFound, InvalidJson, NotFound, UnexpectedResponseStatus}
import models.{SaveForLaterRequest, SavedUserAnswers}
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

class SaveForLaterConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val url: String = "/ioss-netp-registration/save-for-later"

  private val saveForLaterRequest: SaveForLaterRequest = arbitrarySaveForLaterRequest.arbitrary.sample.value

  private val expectedSavedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  private def application = applicationBuilder()
    .configure("microservice.services.ioss-netp-registration.port" -> server.port)
    .build()

  "SaveForLaterConnector" - {

    ".submit" - {

      "must return Right(Some(SavedUserAnswers)) when the server responds with CREATED" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          val responseJson: JsValue = Json.toJson(expectedSavedUserAnswers)

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CREATED)
                .withBody(responseJson.toString()))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(ConflictFound) with server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CONFLICT))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) with server responds with any other error" in {

        val status: Int = INTERNAL_SERVER_ERROR
        val UnexpectedStatusResponseMessage: String = s"Unexpected response received with status: $status."
        val UnexpectedStatusResponse: UnexpectedResponseStatus = UnexpectedResponseStatus(status, UnexpectedStatusResponseMessage)

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Left(UnexpectedStatusResponse)
        }
      }
    }

    ".get" - {

      "must return Right(Some(SavedUserAnswers)) when the server responds with OK" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          val responseJson: JsValue = Json.toJson(expectedSavedUserAnswers)

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString()))
          )

          val result = connector.get().futureValue

          result `mustBe` Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(InvalidJson) when JSON cannot be parsed correctly" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(OK)
                .withBody(Json.toJson("invalidJson").toString))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Right(None) when the server responds with NotFound" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(NOT_FOUND))
          )

          val result = connector.get().futureValue

          result `mustBe` Right(None)
        }
      }

      "must return Left(ConflictFound) with server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CONFLICT))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) with server responds with any other error" in {

        val status: Int = INTERNAL_SERVER_ERROR
        val UnexpectedStatusResponseMessage: String = s"Unexpected response received with status: $status."
        val UnexpectedStatusResponse: UnexpectedResponseStatus = UnexpectedResponseStatus(status, UnexpectedStatusResponseMessage)

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(UnexpectedStatusResponse)
        }
      }
    }

    ".delete" - {

      val url: String = "/ioss-netp-registration/save-for-later/delete"

      "must return Right(true) when server responds with OK" in {

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(true).toString)
            )
        )

        val result = connector.delete().futureValue

        result `mustBe` Right(true)
      }

      "must return Left(InvalidJson) when JSON cannot be parsed correctly" in {

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(Json.toJson("invalidJson").toString)
            )
        )

        val result = connector.delete().futureValue

        result `mustBe` Left(InvalidJson)
      }

      "must return Left(NotFound) with server responds with NotFound" in {

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(NOT_FOUND)
            )
        )

        val result = connector.delete().futureValue

        result `mustBe` Left(NotFound)
      }

      "must return Left(ConflictFound) with server responds with CONFLICT" in {

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(CONFLICT)
            )
        )

        val result = connector.delete().futureValue

        result `mustBe` Left(ConflictFound)
      }

      "must return Left(UnexpectedResponseStatus) with server responds with any other error" in {

        val status: Int = INTERNAL_SERVER_ERROR
        val UnexpectedStatusResponseMessage: String = s"Unexpected response received when deleting saved user answers with status: $status."
        val UnexpectedStatusResponse: UnexpectedResponseStatus = UnexpectedResponseStatus(status, UnexpectedStatusResponseMessage)

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(status)
            )
        )

        val result = connector.delete().futureValue

        result `mustBe` Left(UnexpectedStatusResponse)
      }
    }
  }
}
