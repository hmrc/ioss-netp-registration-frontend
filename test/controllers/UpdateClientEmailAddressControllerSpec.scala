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
import forms.UpdateClientEmailAddressFormProvider
import models.emails.EmailSendingResult
import models.responses.InternalServerError
import models.{BusinessContactDetails, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessContactDetailsPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.EmailService
import views.html.UpdateClientEmailAddressView
import utils.FutureSyntax.FutureOps

class UpdateClientEmailAddressControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockEmailService: EmailService = mock[EmailService]

  val savedPendingRegistration: SavedPendingRegistration = arbitrarySavedPendingRegistration.arbitrary.sample.value
  override val journeyId: String = savedPendingRegistration.journeyId

  val formProvider = new UpdateClientEmailAddressFormProvider()
  private val form = formProvider()

  lazy val updateClientEmailAddressOnPageLoadRoute: String = routes.UpdateClientEmailAddressController.onPageLoad(waypoints, journeyId).url
  lazy val updateClientEmailAddressOnSubmitRoute: String = routes.UpdateClientEmailAddressController.onSubmit(waypoints, journeyId).url

  val companyName: String = vatCustomerInfo.organisationName.get

  override def beforeEach(): Unit =
    reset(mockRegistrationConnector)
    reset(mockEmailService)
  
  "UpdateClientEmailAddress Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val businessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

        val emailAddress = businessContactDetails.emailAddress

        val completeUserAnswers = savedPendingRegistration.userAnswers
          .set(BusinessContactDetailsPage, businessContactDetails).success.value
          .copy(vatInfo = Some(vatCustomerInfo))

        val completePendingRegistration = savedPendingRegistration.copy(userAnswers = completeUserAnswers)

        when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
          .thenReturn(Right(completePendingRegistration).toFuture)

        val application = applicationBuilder(userAnswers = Some(savedPendingRegistration.userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, updateClientEmailAddressOnPageLoadRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UpdateClientEmailAddressView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, journeyId, companyName, emailAddress)(request, messages(application)).toString
        }
      }

      "must throw an exception and log the error when the connector fails to return a pending registration" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
          .thenReturn(Left(InternalServerError).toFuture)

        running(application) {
          val request = FakeRequest(GET, updateClientEmailAddressOnPageLoadRoute)

          val result = route(application, request).value

          intercept[Exception] {
            status(result)
          }
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the acknowledgement page once the email is successfully updated" in {

        val businessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

        val newEmailAddress = "new@email.com"

        val completeUserAnswers = savedPendingRegistration.userAnswers
          .set(BusinessContactDetailsPage, businessContactDetails).success.value
          .copy(vatInfo = Some(vatCustomerInfo))

        val completePendingRegistration = savedPendingRegistration.copy(userAnswers = completeUserAnswers)

        when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
          .thenReturn(Right(completePendingRegistration).toFuture)
        when(mockRegistrationConnector.updateClientEmailAddress(eqTo(journeyId), eqTo(newEmailAddress))(any()))
          .thenReturn(Right(completePendingRegistration).toFuture)
        when(mockEmailService.sendClientActivationEmail(any(), any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(EmailSendingResult.EMAIL_ACCEPTED.toFuture)

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, updateClientEmailAddressOnSubmitRoute)
            .withFormUrlEncodedBody(("value" -> newEmailAddress))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.ClientEmailUpdatedController.onPageLoad(waypoints, journeyId).url
          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(journeyId))(any())
          verify(mockRegistrationConnector, times(1)).updateClientEmailAddress(eqTo(journeyId), eqTo(newEmailAddress))(any())
          verify(mockEmailService, times(1)).sendClientActivationEmail(any(), any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must return a Bad Request error when invalid data is submitted" in {

        val businessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value

        val completeUserAnswers = savedPendingRegistration.userAnswers
          .set(BusinessContactDetailsPage, businessContactDetails).success.value
          .copy(vatInfo = Some(vatCustomerInfo))

        val completePendingRegistration = savedPendingRegistration.copy(userAnswers = completeUserAnswers)

        when(mockRegistrationConnector.getPendingRegistration(eqTo(journeyId))(any()))
          .thenReturn(Right(completePendingRegistration).toFuture)
        when(mockRegistrationConnector.updateClientEmailAddress(eqTo(journeyId), any())(any()))
          .thenReturn(Left(BAD_REQUEST).toFuture)

        val application = applicationBuilder(userAnswers = Some(savedPendingRegistration.userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, updateClientEmailAddressOnSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }
    }
  }
}
