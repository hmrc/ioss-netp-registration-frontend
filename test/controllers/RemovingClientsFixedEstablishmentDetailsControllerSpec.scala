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
import models.Country
import models.vatEuDetails.EuDetails
import models.vatEuDetails.RegistrationType.TaxId
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.vatEuDetails.HasFixedEstablishmentPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import services.SaveAndComeBackService
import utils.FutureSyntax.FutureOps
import views.html.RemovingClientsFixedEstablishmentDetailsView

class RemovingClientsFixedEstablishmentDetailsControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockSaveAndComeBackService = mock[SaveAndComeBackService]

  private val fixedEstablishment = EuDetails(
    euCountry = Country("DE", "Germany"),
    hasFixedEstablishment = Some(true),
    registrationType = Some(TaxId),
    euVatNumber = Some("DK12345678"),
    euTaxReference = Some("ID1"),
    tradingNameAndBusinessAddress = None
  )

  val userAnswers = basicUserAnswersWithVatInfo
    .set(AllEuDetailsQuery, List(fixedEstablishment)).success.value
    .set(HasFixedEstablishmentPage, true).success.value

  override def beforeEach(): Unit = {
    reset(mockSaveAndComeBackService)
  }

  "RemovingClientsFixedEstablishmentDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemovingClientsFixedEstablishmentDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovingClientsFixedEstablishmentDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(clientName = "Company name", waypoints)(request, messages(application)).toString
      }
    }

    "must remove all fixed establishments on POST" in {

      when(mockSaveAndComeBackService.updateSavedUserAnswers(any(), any())(any())) thenReturn ().toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.RemovingClientsFixedEstablishmentDetailsController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad(waypoints).url
        verify(mockSaveAndComeBackService).updateSavedUserAnswers(any(), any())(any())
      }

    }
  }
}
