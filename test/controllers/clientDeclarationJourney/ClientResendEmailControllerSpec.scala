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

package controllers.clientDeclarationJourney

import base.SpecBase
import connectors.RegistrationConnector
import controllers.{clientDeclarationJourney, routes}
import models.domain.VatCustomerInfo
import models.emails.EmailSendingResult
import models.responses.{RegistrationNotFound, UnexpectedResponseStatus}
import models.{BusinessContactDetails, ClientBusinessName, IntermediaryDetails, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, JourneyRecoveryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.IntermediaryDetailsQuery
import services.EmailService

import java.time.Instant
import scala.concurrent.Future

class ClientResendEmailControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val testIntermediaryName = "Test Intermediary Ltd"
  private val testIntermediaryNumber = "Test Intermediary No"
  private val testUniqueUrlCode = "ABC123XYZ"
  private val testClientEmail = "client@example.com"
  private val testActivationCode = "12345"
  private val testLastUpdated = Instant.now()

  private val testBusinessContactDetails = BusinessContactDetails(
    fullName = "Test Contact Name",
    telephoneNumber = "01234567890",
    emailAddress = testClientEmail
  )

  private val clientBusinessName = ClientBusinessName("Test Client Company Ltd")
  private val intermediaryDetails = IntermediaryDetails(testIntermediaryNumber, testIntermediaryName)

  private val baseUserAnswers = emptyUserAnswers
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(IntermediaryDetailsQuery, intermediaryDetails).success.value
    .set(BusinessContactDetailsPage, testBusinessContactDetails).success.value

  private val testSavedPendingRegistration = SavedPendingRegistration(
    journeyId = "test-journey-id",
    uniqueUrlCode = testUniqueUrlCode,
    userAnswers = baseUserAnswers,
    lastUpdated = testLastUpdated,
    uniqueActivationCode = testActivationCode,
    intermediaryDetails = intermediaryDetails
  )

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockEmailService = mock[EmailService]

  private lazy val clientResendEmailRoute =
    clientDeclarationJourney.routes.ClientResendEmailController.onPageLoad(waypoints, testUniqueUrlCode).url

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockEmailService)
  }

  "ClientResendEmail Controller" - {

    ".onPageLoad() - GET" - {

      "must successfully resend email and redirect to ClientCodeEntryController when all data is present" in {
        when(mockRegistrationConnector.getPendingRegistration(eqTo(testUniqueUrlCode))(any()))
          .thenReturn(Future.successful(Right(testSavedPendingRegistration)))

        when(mockEmailService.sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(EmailSendingResult.EMAIL_ACCEPTED))

        val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, testUniqueUrlCode).url

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrlCode))(any())
          verify(mockEmailService, times(1)).sendClientActivationEmail(
            eqTo(testIntermediaryName),
            eqTo(clientBusinessName.name),
            eqTo(testActivationCode),
            eqTo(testSavedPendingRegistration.activationExpiryDate),
            eqTo(testClientEmail)
          )(any(), any())
        }
      }

      "must successfully redirect even when email sending fails" in {
        when(mockRegistrationConnector.getPendingRegistration(eqTo(testUniqueUrlCode))(any()))
          .thenReturn(Future.successful(Right(testSavedPendingRegistration)))

        when(mockEmailService.sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Email service unavailable")))

        val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, testUniqueUrlCode).url

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrlCode))(any())
          verify(mockEmailService, times(1)).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must work with VAT info when ClientBusinessName is not present" in {
        val userAnswersWithVatInfo = emptyUserAnswersWithVatInfo
          .set(IntermediaryDetailsQuery, intermediaryDetails).success.value
          .set(BusinessContactDetailsPage, testBusinessContactDetails).success.value

        val expectedCompanyName = userAnswersWithVatInfo.vatInfo.get.organisationName.get

        when(mockRegistrationConnector.getPendingRegistration(eqTo(testUniqueUrlCode))(any()))
          .thenReturn(Future.successful(Right(testSavedPendingRegistration)))

        when(mockEmailService.sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(EmailSendingResult.EMAIL_ACCEPTED))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithVatInfo))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          verify(mockEmailService, times(1)).sendClientActivationEmail(
            eqTo(testIntermediaryName),
            eqTo(expectedCompanyName),
            eqTo(testActivationCode),
            eqTo(testSavedPendingRegistration.activationExpiryDate),
            eqTo(testClientEmail)
          )(any(), any())
        }
      }

      "must redirect to ClientCodeEntryController when registration connector returns RegistrationNotFound" in {
        when(mockRegistrationConnector.getPendingRegistration(eqTo(testUniqueUrlCode))(any()))
          .thenReturn(Future.successful(Left(RegistrationNotFound)))

        val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, testUniqueUrlCode).url

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrlCode))(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to ClientCodeEntryController when registration connector returns UnexpectedResponseStatus" in {
        when(mockRegistrationConnector.getPendingRegistration(eqTo(testUniqueUrlCode))(any()))
          .thenReturn(Future.successful(Left(UnexpectedResponseStatus(500, "Internal Server Error"))))

        val application = applicationBuilder(userAnswers = Some(baseUserAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, testUniqueUrlCode).url

          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrlCode))(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when client email is missing" in {
        val userAnswersWithoutEmail = emptyUserAnswers
          .set(ClientBusinessNamePage, clientBusinessName).success.value
          .set(IntermediaryDetailsQuery, intermediaryDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutEmail))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationConnector, never()).getPendingRegistration(any())(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when intermediary details are missing" in {
        val userAnswersWithoutIntermediary = emptyUserAnswers
          .set(ClientBusinessNamePage, clientBusinessName).success.value
          .set(BusinessContactDetailsPage, testBusinessContactDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutIntermediary))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url

          verify(mockRegistrationConnector, never()).getPendingRegistration(any())(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when client company name is missing" in {
        val userAnswersWithoutCompanyName = emptyUserAnswers
          .set(IntermediaryDetailsQuery, intermediaryDetails).success.value
          .set(BusinessContactDetailsPage, testBusinessContactDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutCompanyName))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url

          verify(mockRegistrationConnector, never()).getPendingRegistration(any())(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when no user answer data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockRegistrationConnector, never()).getPendingRegistration(any())(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }

      "must redirect to Journey Recovery when VAT info has no organisation or individual name" in {
        val vatInfoWithoutNames = arbitraryVatCustomerInfo.arbitrary.sample.value.copy(
          organisationName = None,
          individualName = None
        )

        val userAnswersWithIncompleteVatInfo = emptyUserAnswers.copy(vatInfo = Some(vatInfoWithoutNames))
          .set(IntermediaryDetailsQuery, intermediaryDetails).success.value
          .set(BusinessContactDetailsPage, testBusinessContactDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithIncompleteVatInfo))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[EmailService].toInstance(mockEmailService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, clientResendEmailRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url

          verify(mockRegistrationConnector, never()).getPendingRegistration(any())(any())
          verify(mockEmailService, never()).sendClientActivationEmail(any(), any(), any(), any(), any())(any(), any())
        }
      }
    }
  }
}