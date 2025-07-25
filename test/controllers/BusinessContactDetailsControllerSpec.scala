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
import forms.BusinessContactDetailsFormProvider
import models.{BusinessContactDetails, ClientBusinessName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, ClientVatNumberPage, JourneyRecoveryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.BusinessContactDetailsView

class BusinessContactDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new BusinessContactDetailsFormProvider()
  private val form: Form[BusinessContactDetails] = formProvider()

  private val clientBusinessName: ClientBusinessName = ClientBusinessName(vatCustomerInfo.organisationName.value)
  
  private lazy val businessContactDetailsRoute: String = routes.BusinessContactDetailsController.onPageLoad(waypoints).url
  
  private val userAnswers = emptyUserAnswersWithVatInfo
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(ClientVatNumberPage, vatNumber).success.value

  "BusinessContactDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .build()
      
      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      
      val answers = userAnswers
        .set(BusinessContactDetailsPage, businessContactDetails).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(businessContactDetails), waypoints, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, businessContactDetailsRoute)
            .withFormUrlEncodedBody(
              ("fullName", businessContactDetails.fullName),
              ("telephoneNumber", businessContactDetails.telephoneNumber),
              ("emailAddress", businessContactDetails.emailAddress)
            )

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = userAnswers
          .set(ClientBusinessNamePage, clientBusinessName).success.value
          .set(BusinessContactDetailsPage, businessContactDetails).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` BusinessContactDetailsPage.navigate(waypoints, userAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, businessContactDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[BusinessContactDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, clientBusinessName.name)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, businessContactDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, businessContactDetailsRoute)
            .withFormUrlEncodedBody(
              ("fullName", businessContactDetails.fullName),
              ("telephoneNumber", businessContactDetails.telephoneNumber),
              ("emailAddress", businessContactDetails.emailAddress)
            )

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
