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
import models.{PendingRegistrationRequest, SavedPendingRegistration, UserAnswers}
import models.domain.VatCustomerInfo
import models.etmp.display.RegistrationWrapper
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.{OssExcludedTrader, OssRegistration}
import models.responses.*
import models.responses.etmp.EtmpEnrolmentResponse
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.{Json, OWrites}
import play.api.test.Helpers.running
import testutils.RegistrationData.etmpRegistrationRequest
import testutils.WireMockHelper
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val userAnswers: UserAnswers = arbitraryUserAnswers.arbitrary.sample.value
  private val pendingRegistrationRequest: PendingRegistrationRequest = PendingRegistrationRequest(
    userAnswers = userAnswers,
    intermediaryDetails = intermediaryDetails
  )
  private val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value

  private val otherErrorStatuses: Seq[Int] = Seq(BAD_REQUEST, UNSUPPORTED_MEDIA_TYPE, UNPROCESSABLE_ENTITY)
  private val vatNumber = "123456789"

  private def application: Application = applicationBuilder()
    .configure(
      "microservice.services.ioss-netp-registration.port" -> server.port(),
      "microservice.services.ioss-intermediary-registration.port" -> server.port,
      "microservice.services.one-stop-shop-registration.port" -> server.port,
      "microservice.services.ioss-registration.port" -> server.port
    )
    .build()

  "RegistrationConnector" - {

    ".getCustomerVatInfo" - {

      val url: String = "/ioss-netp-registration/vat-information/123456789"

      "must return vat information when the backend returns some" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val vatInfo: VatCustomerInfo = vatCustomerInfo

          val responseBody = Json.toJson(vatInfo).toString()

          server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

          val result = connector.getVatCustomerInfo(vatNumber).futureValue

          result `mustBe` Right(vatInfo)
        }
      }

      "must return invalid json when the backend returns some" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val responseBody = Json.obj("test" -> "test").toString()

          server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

          val result = connector.getVatCustomerInfo(vatNumber).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(NotFound) when the backend returns NOT_FOUND" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

          val result = connector.getVatCustomerInfo(vatNumber).futureValue

          result `mustBe` Left(VatCustomerNotFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the backend returns another error code" in {

        val status = Gen.oneOf(BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE).sample.value

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

          val result = connector.getVatCustomerInfo(vatNumber).futureValue

          result `mustBe` Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
        }
      }
    }

    ".getIntermediaryVatCustomerInfo" - {

      val url: String = "/ioss-intermediary-registration/vat-information"

      "must return vat information when the backend returns some" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val vatInfo: VatCustomerInfo = intermediaryVatCustomerInfo

          val responseBody = Json.toJson(vatInfo).toString()

          server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

          val result = connector.getIntermediaryVatCustomerInfo().futureValue

          result `mustBe` Right(vatInfo)
        }
      }

      "must return invalid json when the backend returns some" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val responseBody = Json.obj("test" -> "test").toString()

          server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

          val result = connector.getIntermediaryVatCustomerInfo().futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(NotFound) when the backend returns NOT_FOUND" in {

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

          val result = connector.getIntermediaryVatCustomerInfo().futureValue

          result `mustBe` Left(VatCustomerNotFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the backend returns another error code" in {

        val status = Gen.oneOf(BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE).sample.value

        running(application) {
          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

          val result = connector.getIntermediaryVatCustomerInfo().futureValue

          result `mustBe` Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
        }
      }
    }

    ".submitPendingRegistration" - {

      val url: String = "/ioss-netp-registration/save-pending-registration"

      "must return Right when a new Pending registration is created on the backend" in {
        val responseBody = Json.toJson(savedPendingRegistration).toString

        running(application) {

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(ok().withBody(responseBody))
          )

          val result = connector.submitPendingRegistration(pendingRegistrationRequest).futureValue

          result `mustBe` Right(savedPendingRegistration)
        }
      }

      otherErrorStatuses.foreach { status =>

        s"must return Left(UnexpectedResponseStatus) when the server returns status: $status" in {

          val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to create the pending registration, status $status returned")

          running(application) {

            val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              post(urlEqualTo(url))
                .willReturn(aResponse()
                  .withStatus(status))
            )

            val result = connector.submitPendingRegistration(pendingRegistrationRequest).futureValue

            result `mustBe` Left(response)
          }
        }
      }
    }

    ".getPendingRegistration" - {

      val journeyId: String = savedPendingRegistration.journeyId

      val url: String = s"/ioss-netp-registration/save-pending-registration/$journeyId"

      "must return a Right(Some(SavedPendingRegistration)) for a given journeyId when the server returns an existing one" in {

        val responseBody = Json.toJson(savedPendingRegistration).toString

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok()
                .withBody(responseBody))
          )

          val result = connector.getPendingRegistration(journeyId).futureValue

          result `mustBe` Right(savedPendingRegistration)
        }
      }

      otherErrorStatuses.foreach { status =>
        s"must return Left(UnexpectedResponseStatus) when the server returns status: $status" in {

          val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to retrieve the pending registration, status $status returned")

          running(application) {

            val connector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              get(urlEqualTo(url))
                .willReturn(aResponse()
                  .withStatus(status))
            )

            val result = connector.getPendingRegistration(journeyId).futureValue

            result `mustBe` Left(response)
          }
        }
      }
    }

    ".getPendingRegistrationsByIntermediaryNumber" - {

      val url: String = s"/ioss-netp-registration/pending-registrations/$intermediaryNumber"

      "must return a Right(Seq(SavedPendingRegistration)) for a given intermediaryNumber when pending registrations exist in the database" in {

        val responseBody = Json.toJson(Seq(savedPendingRegistration)).toString

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok()
                .withBody(responseBody))
          )

          val result = connector.getPendingRegistrationsByIntermediaryNumber(intermediaryNumber).futureValue

          result `mustBe` Right(Seq(savedPendingRegistration))
        }
      }

      otherErrorStatuses.foreach { status =>
        s"must return Left(UnexpectedResponseStatus) when the server returns status: $status" in {

          val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to retrieve pending registrations, status $status returned")

          running(application) {

            val connector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              get(urlEqualTo(url))
                .willReturn(aResponse()
                  .withStatus(status))
            )

            val result = connector.getPendingRegistrationsByIntermediaryNumber(intermediaryNumber).futureValue

            result `mustBe` Left(response)
          }
        }
      }
    }

    ".deletePendingRegistration" - {

      val url: String = s"/ioss-netp-registration/pending-registrations/$journeyId"

      "must return true when a pending registration is successfully deleted" in {

          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            delete(urlEqualTo(url))
              .willReturn(ok()
              .withBody("true"))
          )

          val result = connector.deletePendingRegistration(journeyId).futureValue

          result mustEqual true
      }
    }

    ".validateClientCode" - {

      val uniqueUrlCode: String = savedPendingRegistration.uniqueUrlCode
      val uniqueActivationCode: String = savedPendingRegistration.uniqueActivationCode

      val url: String = s"/ioss-netp-registration/validate-pending-registration/$uniqueUrlCode/$uniqueActivationCode"

      "must return a Right(true) for a matching urlCode and activationCode when the server returns true" in {

        val responseBody = Json.toJson(true).toString

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok()
                .withBody(responseBody))
          )

          val result = connector.validateClientCode(uniqueUrlCode, uniqueActivationCode).futureValue

          result `mustBe` Right(true)
        }
      }

      "must return a Right(false) for a none matching urlCode and activationCode when the server returns false" in {

        val responseBody = Json.toJson(false).toString

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(ok()
                .withBody(responseBody))
          )

          val result = connector.validateClientCode(uniqueUrlCode, uniqueActivationCode).futureValue

          result `mustBe` Right(false)
        }
      }

      otherErrorStatuses.foreach { status =>
        s"must return Left(UnexpectedResponseStatus) when the server returns status: $status" in {

          val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to validate registration code, status $status returned")

          running(application) {

            val connector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              get(urlEqualTo(url))
                .willReturn(aResponse()
                  .withStatus(status))
            )

            val result = connector.validateClientCode(uniqueUrlCode, uniqueActivationCode).futureValue

            result `mustBe` Left(response)
          }
        }
      }
    }

    ".getOssRegistration" - {

      implicit val ossExcludedTraderWrites: OWrites[OssExcludedTrader] = Json.writes[OssExcludedTrader]
      implicit val ossRegistrationWrites: OWrites[OssRegistration] = Json.writes[OssRegistration]
      val vrn = "123456789"
      val url = s"/one-stop-shop-registration/registration/$vrn"

      "must return Right(OssRegistration) when the backend returns valid data" in {

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]
          val ossRegistration = arbitrary[OssRegistration].sample.value
          val responseBody = Json.toJson(ossRegistration).toString

          server.stubFor(
            get(urlEqualTo(url)).willReturn(ok().withBody(responseBody))
          )

          val result = connector.getOssRegistration(Vrn(vrn)).futureValue

          result `mustBe` Right(ossRegistration)
        }
      }

      "must return Left(InvalidJson) when the backend returns malformed JSON" in {
        val malformedJson = Json.obj("unexpectedField" -> "value").toString()

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url)).willReturn(ok().withBody(malformedJson))
          )

          val result = connector.getOssRegistration(Vrn(vrn)).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(InternalServerError) when backend returns an error response" in {
        val status = INTERNAL_SERVER_ERROR

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url)).willReturn(aResponse().withStatus(status))
          )

          val result = connector.getOssRegistration(Vrn(vrn)).futureValue

          result `mustBe` Left(InternalServerError)
        }
      }

      otherErrorStatuses.foreach { status =>
        s"must return Left(InternalServerError) when backend returns status $status" in {

          running(application) {
            val connector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              get(urlEqualTo(url)).willReturn(aResponse().withStatus(status))
            )

            val result = connector.getOssRegistration(Vrn(vrn)).futureValue

            result `mustBe` Left(InternalServerError)
          }
        }
      }
    }

    ".getIossRegistration" - {

      val iossNumber = "1234567890"
      val url = s"/ioss-registration/registration-as-intermediary/$iossNumber"

      "must return Right(IossEtmpDisplayRegistration) when the backend returns valid data" in {
        val iossReg = arbitrary[IossEtmpDisplayRegistration].sample.get
        val responseBody = Json.toJson(iossReg).toString()

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url)).willReturn(ok().withBody(responseBody))
          )

          val result = connector.getIossRegistration(iossNumber).futureValue

          result `mustBe` Right(iossReg)
        }
      }

      "must return Left(InvalidJson) when the backend returns malformed JSON" in {
        val malformedJson = Json.obj("registration" -> Json.obj("foo" -> "bar")).toString()

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url)).willReturn(ok().withBody(malformedJson))
          )

          val result = connector.getIossRegistration(iossNumber).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the backend returns an error response" in {
        val status = INTERNAL_SERVER_ERROR

        running(application) {
          val connector = application.injector.instanceOf[RegistrationConnector]

          server.stubFor(
            get(urlEqualTo(url)).willReturn(aResponse().withStatus(status))
          )

          val result = connector.getIossRegistration(iossNumber).futureValue

          result `mustBe` Left(UnexpectedResponseStatus(status, s"Unexpected IOSS registration response, status $status returned"))
        }
      }

      otherErrorStatuses.foreach { status =>
        s"must return Left(UnexpectedResponseStatus) when backend returns status $status" in {

          running(application) {
            val connector = application.injector.instanceOf[RegistrationConnector]

            server.stubFor(
              get(urlEqualTo(url)).willReturn(aResponse().withStatus(status))
            )

            val result = connector.getIossRegistration(iossNumber).futureValue

            result `mustBe` Left(UnexpectedResponseStatus(status, s"Unexpected IOSS registration response, status $status returned"))
          }
        }
      }
    }

    ".createRegistration" - {

      val url: String = "/ioss-netp-registration/create-registration"

      "must return CREATED with a valid ETMP Enrolment Response when backend returns CREATED with a valid response body" in {

        running(application) {

          val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

          val responseBody = Json.toJson(etmpEnrolmentResponse).toString()

          server.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse.withStatus(CREATED)
              .withBody(responseBody)))

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.createRegistration(etmpRegistrationRequest).futureValue

          result mustBe Right(etmpEnrolmentResponse)
        }
      }

      "must return Left(InvalidJson) when the response body is not a valid ETMP Enrolment Response Json" in {

        running(application) {

          server.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse.withStatus(CREATED)
              .withBody(Json.toJson("test").toString())))

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.createRegistration(etmpRegistrationRequest).futureValue

          result mustBe Left(InvalidJson)
        }
      }

      "must return Conflict when backend responds with Conflict" in {

        running(application) {

          server.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse.withStatus(CONFLICT)))

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.createRegistration(etmpRegistrationRequest).futureValue

          result mustBe Left(ConflictFound)
        }
      }

      "must return Internal Server Error when backend responds with Internal Server Error" in {

        running(application) {

          server.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse.withStatus(INTERNAL_SERVER_ERROR)))

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.createRegistration(etmpRegistrationRequest).futureValue

          result mustBe Left(InternalServerError)
        }
      }

      "must return UnexpectedResponseStatus when any other error is returned" in {

        val status = 123

        running(application) {

          server.stubFor(post(urlEqualTo(url))
            .willReturn(aResponse.withStatus(status)))

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.createRegistration(etmpRegistrationRequest).futureValue

          result mustBe Left(UnexpectedResponseStatus(status, s"Unexpected response, status $status returned"))
        }
      }
    }

    ".updateClientEmailAddress" - {

      val journeyId = savedPendingRegistration.journeyId
      val oldEmail = "old@email.com"
      val newEmail = "new@email.com"

      val savedPendingRegistrationWithOldEmail: SavedPendingRegistration =
        savedPendingRegistration.copy(
          userAnswers = savedPendingRegistration.userAnswers.copy(
            data = Json.obj(
              "businessContactDetails" -> Json.obj(
                "emailAddress" -> oldEmail
              )
            )
          )
        )

      val url: String = s"/ioss-netp-registration/pending-registrations/$journeyId/$newEmail"

      "must return an updated SavedPendingRegistration response" in {

        val updatedPendingRegistration =
          savedPendingRegistrationWithOldEmail.copy(
            userAnswers = savedPendingRegistrationWithOldEmail.userAnswers.copy(
              data = Json.obj(
                "businessContactDetails" -> Json.obj(
                  "emailAddress" -> newEmail
                )
              )
            )
          )

        running(application) {

          val responseBody = Json.toJson(updatedPendingRegistration).toString

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(OK)
                .withBody(responseBody))
          )

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.updateClientEmailAddress(journeyId, newEmail).futureValue

          result mustBe Right(updatedPendingRegistration)
        }
      }

      "must return InvalidJson error when the response body is not valid" in {

        running(application) {

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(aResponse()
                .withBody(Json.toJson("test").toString()))
          )

          val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.updateClientEmailAddress(journeyId, newEmail).futureValue

          result mustBe Left(InvalidJson)
        }
      }

      otherErrorStatuses.foreach { status =>

        s"must return UnexpectedResponseStatus when the server returns status: $status" in {

          val response = UnexpectedResponseStatus(status, s"Unexpected response when trying to update the pending registration, status $status returned")

          running(application) {

            server.stubFor(
              put(urlEqualTo(url))
                .willReturn(aResponse().withStatus(status))
            )

            val connector: RegistrationConnector = application.injector.instanceOf[RegistrationConnector]

            val result = connector.updateClientEmailAddress(journeyId, newEmail).futureValue

            result mustBe Left(response)
          }
        }
      }
    }

    ".displayRegistration" - {

      val displayRegistrationUrl: String = s"/ioss-netp-registration/registrations/$intermediaryNumber"

      val displayRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

      "must return Right(RegistrationWrapper) when the server returns a successful response and JSON is parsed correctly" in {

        val responseJson = Json.toJson(displayRegistrationWrapper).toString

        server.stubFor(
          get(urlEqualTo(displayRegistrationUrl))
            .willReturn(aResponse()
            .withStatus(OK)
            .withBody(responseJson))
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.displayRegistration(intermediaryNumber).futureValue

          result `mustBe` Right(displayRegistrationWrapper)
        }

      }

      "must return Left(InvalidJson) when when JSON is not parsed correctly" in {

        val responseJson = Json.toJson("JSON" -> "Invalid").toString

        server.stubFor(
          get(urlEqualTo(displayRegistrationUrl))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(responseJson))
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.displayRegistration(intermediaryNumber).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Left(InternalServerError) when server responds with an error" in {

        server.stubFor(
          get(urlEqualTo(displayRegistrationUrl))
            .willReturn(aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        running(application) {

          val connector = application.injector.instanceOf[RegistrationConnector]

          val result = connector.displayRegistration(intermediaryNumber).futureValue

          result `mustBe` Left(InternalServerError)
        }
      }
    }
  }
}
