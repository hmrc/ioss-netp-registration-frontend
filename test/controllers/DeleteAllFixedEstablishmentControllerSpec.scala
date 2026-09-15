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
import connectors.RegistrationConnector
import models.etmp.amend.AmendRegistrationResponse
import models.etmp.intermediary.IntermediaryRegistrationWrapper
import models.{Country, InternationalAddress, TradingName}
import models.vatEuDetails.{EuDetails, TradingNameAndBusinessAddress}
import models.vatEuDetails.RegistrationType.TaxId
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.DeleteAllFixedEstablishmentPage
import pages.vatEuDetails.HasFixedEstablishmentPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import services.RegistrationService
import utils.FutureSyntax.FutureOps
import views.html.DeleteAllFixedEstablishmentView

import java.time.LocalDateTime


class DeleteAllFixedEstablishmentControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockRegistrationService = mock[RegistrationService]

  private val fixedEstablishment = EuDetails(
    euCountry = Country("DE", "Germany"),
    hasFixedEstablishment = Some(true),
    registrationType = Some(TaxId),
    euVatNumber = Some("DK12345678"),
    euTaxReference = Some("ID1"),
    tradingNameAndBusinessAddress = Some(
      TradingNameAndBusinessAddress(
        tradingName = TradingName("fixedEstablishmentTradingName"),
        address = InternationalAddress(
          line1 = "Line 1",
          line2 = Some("Line 2"),
          townOrCity = "Town",
          stateOrRegion = Some("Region"),
          postCode = Some("AB12 3CD"),
          country = Some(Country("DE", "Germany"))
        )
      )
    )
  )

  def intermediaryRegistrationWithClients(iossNumbers: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryEtmpDisplayIntermediaryRegistration.arbitrary.sample.value.copy(
        clientDetails = iossNumbers.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
      )
    )
  }

  private val baseUserAnswers = basicUserAnswersWithVatInfo

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector, mockRegistrationService)
  }

  "DeleteAllFixedEstablishment Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
        Right(intermediaryRegistrationWithClients(Seq(iossNumber))).toFuture
      when(mockRegistrationConnector.displayRegistrationNetp(eqTo(iossNumber))(any())) thenReturn Right(registrationWrapper).toFuture
      when(mockRegistrationService.toUserAnswers(any(), any())) thenReturn baseUserAnswers.toFuture
      when(mockRegistrationService.convertEuFixedEstablishmentDetails(any())) thenReturn Seq(fixedEstablishment)

      val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.DeleteAllFixedEstablishmentController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllFixedEstablishmentView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Seq(fixedEstablishment), waypoints, iossNumber)(request, messages(application)).toString
      }
    }

    "must remove all fixed establishments on POST" in {

      val amendRegistrationResponse: AmendRegistrationResponse = AmendRegistrationResponse(
        processingDateTime = LocalDateTime.now(),
        formBundleNumber = "123456789",
        iossReference = "IM900123456",
        businessPartner = "Test Business Partner"
      )

      when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn
        Right(intermediaryRegistrationWithClients(Seq(iossNumber))).toFuture
      when(mockRegistrationConnector.displayRegistrationNetp(eqTo(iossNumber))(any())) thenReturn
        Right(registrationWrapper).toFuture
      when(mockRegistrationService.amendRegistration(any(), any(), any(), any())(any())) thenReturn
        Right(amendRegistrationResponse).toFuture

      val userAnswers = baseUserAnswers.set(AllEuDetailsQuery, List(fixedEstablishment)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.DeleteAllFixedEstablishmentController.onSubmit(waypoints, iossNumber).url)

        val result = route(application, request).value

        val expectedUpdatedAnswers = userAnswers
          .set(AllEuDetailsQuery, List.empty[EuDetails]).success.value
          .set(HasFixedEstablishmentPage, false).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual DeleteAllFixedEstablishmentPage(iossNumber).navigate(waypoints, userAnswers, expectedUpdatedAnswers).route.url
      }

    }
  }
}
