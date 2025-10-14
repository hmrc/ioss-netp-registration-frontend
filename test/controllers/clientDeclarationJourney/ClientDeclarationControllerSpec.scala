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
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import models.audit.DeclarationSigningAuditType.CreateClientDeclaration
import models.audit.SubmissionResult.{Failure, Success}
import models.audit.{DeclarationSigningAuditModel, DeclarationSigningAuditType, RegistrationAuditModel, SubmissionResult}
import models.requests.ClientOptionalDataRequest
import models.responses.InternalServerError as ServerError
import models.responses.etmp.EtmpEnrolmentResponse
import models.{ClientBusinessName, IntermediaryDetails, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.clientDeclarationJourney.ClientDeclarationPage
import pages.{ClientBusinessNamePage, EmptyWaypoints, ErrorSubmittingRegistrationPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.IntermediaryDetailsQuery
import queries.etmp.EtmpEnrolmentResponseQuery
import repositories.SessionRepository
import services.{AuditService, RegistrationService}
import utils.FutureSyntax.FutureOps
import views.html.clientDeclarationJourney.ClientDeclarationView

class ClientDeclarationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val clientBusinessName: ClientBusinessName = arbitraryClientBusinessName.arbitrary.sample.value
  lazy val clientDeclarationOnPageLoad: String = clientDeclarationJourney.routes.ClientDeclarationController.onPageLoad(waypoints).url
  lazy val clientDeclarationOnSubmit: String = clientDeclarationJourney.routes.ClientDeclarationController.onSubmit(waypoints).url

  val completeUserAnswers: UserAnswers = emptyUserAnswers
    .set(ClientBusinessNamePage, clientBusinessName).success.value
    .set(IntermediaryDetailsQuery, intermediaryDetails)
    .success.value

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAuditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = reset(
    mockRegistrationConnector,
    mockAuditService
  )

  val formProvider = new ClientDeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  "ClientDeclaration Controller" - {

    ".onPageLoad() - GET must " - {

      "return OK and the correct view with all userAnswers" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientDeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, clientBusinessName.name, intermediaryDetails.intermediaryName)(request, messages(application)).toString
        }
      }

      "return OK and the correct view with missing ClientBusinessName information" in {
        val userAnswersWithVatInfo: UserAnswers = emptyUserAnswersWithVatInfo
          .set(IntermediaryDetailsQuery, intermediaryDetails)
          .success.value

        val clientBusinessName: Option[String] = emptyUserAnswersWithVatInfo.vatInfo.get.organisationName

        val application = applicationBuilder(userAnswers = Some(userAnswersWithVatInfo)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientDeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, clientBusinessName.get, intermediaryDetails.intermediaryName)(request, messages(application)).toString
        }
      }

      "return OK and the correct view with missing vat information" in {
        val userAnswersWithCompanyName: UserAnswers = emptyUserAnswers
          .set(ClientBusinessNamePage, clientBusinessName).success.value
          .set(IntermediaryDetailsQuery, intermediaryDetails)
          .success.value
        val application = applicationBuilder(userAnswers = Some(userAnswersWithCompanyName)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ClientDeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, waypoints, clientBusinessName.name, intermediaryDetails.intermediaryName)(request, messages(application)).toString
        }
      }

      "return OK and the correct view when the question has previously been answered" in {

        val filledUserAnswers = completeUserAnswers
          .set(ClientDeclarationPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(filledUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val view = application.injector.instanceOf[ClientDeclarationView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), waypoints, clientBusinessName.name, intermediaryDetails.intermediaryName)(request, messages(application)).toString
        }
      }

      "return an error when userAnswers are missing Intermediary information" in {
        val userAnswersWithoutIntermediaryInfo: UserAnswers = emptyUserAnswersWithVatInfo
          .set(ClientBusinessNamePage, clientBusinessName).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutIntermediaryInfo)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "return an error when userAnswers are missing Client Company Information" in {
        val userAnswersWithoutCompanyInfo: UserAnswers = emptyUserAnswers
          .set(IntermediaryDetailsQuery, intermediaryDetails)
          .success.value

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutCompanyInfo)).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing userAnswer data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationOnPageLoad)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

    ".onSubmit() - POST must" - {

      "audit event and redirect to the next page when valid data is submitted and submit downstream" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockRegistrationService = mock[RegistrationService]
        val mockRegistrationConnector = mock[RegistrationConnector]
        val mockClientDeclarationView = mock[ClientDeclarationView]
        val viewMock = mock[play.twirl.api.HtmlFormat.Appendable]
        val testViewBody = "test-view-body"

        val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(iossReference = "123456789")

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture
        when(mockRegistrationConnector.deletePendingRegistration(any())(any())) thenReturn true.toFuture
        when(mockClientDeclarationView.apply(any(), any(), any(), any())(any(), any())) thenReturn viewMock
        when(viewMock.body) thenReturn testViewBody

        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[RegistrationConnector].toInstance(mockRegistrationConnector),
              bind[AuditService].toInstance(mockAuditService),
              bind[ClientDeclarationView].toInstance(mockClientDeclarationView),
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("declaration", "true"))

          val result = route(application, request).value

          implicit val dataRequest: ClientOptionalDataRequest[_] = {
            ClientOptionalDataRequest(request, userAnswersId, completeUserAnswers)
          }

          val expectedAnswers = completeUserAnswers
            .set(ClientDeclarationPage, true).success.value
            .set(EtmpEnrolmentResponseQuery, etmpEnrolmentResponse).success.value

          val expectedAuditEvent: DeclarationSigningAuditModel = DeclarationSigningAuditModel.build(
            CreateClientDeclaration, expectedAnswers, Success, testViewBody
          )

          val expectedRegistrationAuditEvent = RegistrationAuditModel.build(
            completeUserAnswers,
            Some(etmpEnrolmentResponse),
            SubmissionResult.Success
          )

          doNothing().when(mockAuditService).audit(eqTo(expectedAuditEvent))(any(), any())
          doNothing().when(mockAuditService).audit(eqTo(expectedRegistrationAuditEvent))(any(), any())

          redirectLocation(result).value mustEqual ClientDeclarationPage.navigate(EmptyWaypoints, completeUserAnswers, expectedAnswers).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          verify(mockAuditService, times(1)).audit(eqTo(expectedRegistrationAuditEvent))(any(), any())
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
          verify(mockClientDeclarationView, times(1))
            .apply(
              form = eqTo(form.fill(true)),
              waypoints = any(),
              clientName = eqTo(clientBusinessName.name),
              intermediaryName = eqTo(intermediaryDetails.intermediaryName)
            )(any(), any())
        }
      }

      "must save the answers, audit event and redirect the Error Submitting Registration page when back end returns any other Error Response" in {

        val mockSessionRepository = mock[SessionRepository]
        val mockRegistrationService = mock[RegistrationService]
        val mockClientDeclarationView = mock[ClientDeclarationView]
        val viewMock = mock[play.twirl.api.HtmlFormat.Appendable]
        val testViewBody = "test-view-body"

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.createRegistration(any())(any())) thenReturn Left(ServerError).toFuture
        when(mockClientDeclarationView.apply(any(), any(), any(), any())(any(), any())) thenReturn viewMock
        when(viewMock.body) thenReturn testViewBody

        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[RegistrationService].toInstance(mockRegistrationService),
              bind[AuditService].toInstance(mockAuditService),
              bind[ClientDeclarationView].toInstance(mockClientDeclarationView),
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("declaration", "true"))

          val result = route(application, request).value

          implicit val dataRequest: ClientOptionalDataRequest[_] = {
            ClientOptionalDataRequest(request, userAnswersId, completeUserAnswers)
          }

          val expectedAuditEvent: DeclarationSigningAuditModel = DeclarationSigningAuditModel.build(
            CreateClientDeclaration, completeUserAnswers, Failure, testViewBody
          )

          val expectedRegistrationAuditEvent = RegistrationAuditModel.build(
            completeUserAnswers,
            None,
            SubmissionResult.Failure
          )

          doNothing().when(mockAuditService).audit(eqTo(expectedAuditEvent))(any(), any())
          doNothing().when(mockAuditService).audit(eqTo(expectedRegistrationAuditEvent))(any(), any())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ErrorSubmittingRegistrationPage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          verify(mockAuditService, times(1)).audit(eqTo(expectedRegistrationAuditEvent))(any(), any())
          verifyNoInteractions(mockSessionRepository)
          verify(mockClientDeclarationView, times(1))
            .apply(
              form = eqTo(form.fill(true)),
              waypoints = any(),
              clientName = eqTo(clientBusinessName.name),
              intermediaryName = eqTo(intermediaryDetails.intermediaryName)
            )(any(), any())
        }
      }

      "return a Bad Request and errors when invalid data is submitted" in {
        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[ClientDeclarationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, waypoints, clientBusinessName.name, intermediaryDetails.intermediaryName)(request, messages(application)).toString
        }
      }

      "redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, clientDeclarationOnSubmit)
              .withFormUrlEncodedBody(("value[0]", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
