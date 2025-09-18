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
import connectors.RegistrationConnector
import models.responses.InternalServerError
import models.{BusinessContactDetails, IntermediaryDetails, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessBasedInUKPage, BusinessContactDetailsPage, ClientHasVatNumberPage, ClientVatNumberPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps


class ClientNotActivatedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]

  def generate6DigitCode(): String = {
    util.Random.alphanumeric.filter(_.isUpper).take(6).mkString
  }

  private def savedPendingRegistration(userAnswers: UserAnswers): SavedPendingRegistration =
    SavedPendingRegistration(
      journeyId = journeyId,
      uniqueUrlCode = generate6DigitCode(),
      userAnswers = userAnswers,
      lastUpdated = emptyUserAnswers.lastUpdated,
      uniqueActivationCode = generate6DigitCode(),
      intermediaryDetails = IntermediaryDetails(intermediaryNumber, intermediaryName)
    )

  private def buildSavedRegistration(
                                      withVatInfo: Boolean = true,
                                      isBasedInUk: Boolean = true,
                                      hasVat: Boolean = true
                                    ): SavedPendingRegistration = {
    val ua = emptyUserAnswers
      .set(BusinessContactDetailsPage, businessContactDetails).success.value
      .set(BusinessBasedInUKPage, isBasedInUk).success.value
      .set(ClientHasVatNumberPage, hasVat).success.value
      .set(ClientVatNumberPage, vatNumber).success.value
      .copy(vatInfo = if (withVatInfo) Some(vatCustomerInfo) else None)

    savedPendingRegistration(ua).copy(
      journeyId = journeyId
    )
  }

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "ClientNotActivated Controller" - {

    "must return OK and the correct view for a GET" - {

      "when journeyId is found, user is UK based, has VAT, and vatInfo exists" in {

        val registration = buildSavedRegistration()

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(registration).toFuture)

        val app = applicationBuilder(userAnswers = None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.ClientNotActivatedController.onPageLoad(waypoints, journeyId).url)

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(vatCustomerInfo.organisationName.get)
          contentAsString(result) must include(vatNumber)

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(any())(any())
        }
      }

      "when journeyId is found, user is UK based but does not have a VAT number" in {

        val registration = buildSavedRegistration(hasVat = false)

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(registration).toFuture)

        val app = applicationBuilder(userAnswers = None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.ClientNotActivatedController.onPageLoad(waypoints, journeyId).url)

          val result = route(app, request).value

          status(result) mustEqual OK

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(any())(any())
        }
      }

      "when journeyId is found but the user is not UK-based or has no VAT" in {

        val registration = buildSavedRegistration(isBasedInUk = false, hasVat = false)

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(registration).toFuture)

        val app = applicationBuilder(userAnswers = None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.ClientNotActivatedController.onPageLoad(waypoints, journeyId).url)

          val result = route(app, request).value

          status(result) mustEqual OK
          contentAsString(result) must include(vatCustomerInfo.organisationName.get)
          contentAsString(result) must include(businessContactDetails.emailAddress)
        }
      }

    }

    "must throw an exception when the connector fails to return pending registrations" in {

      when(mockRegistrationConnector.getPendingRegistration(any())(any()))
        .thenReturn(Left(InternalServerError).toFuture)

      val app = applicationBuilder(userAnswers = None)
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.ClientNotActivatedController.onPageLoad(waypoints, journeyId).url)

        assertThrows[Exception] {
          route(app, request).value.futureValue
        }
      }
    }
  }
}
