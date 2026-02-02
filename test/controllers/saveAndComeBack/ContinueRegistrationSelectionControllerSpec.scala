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

package controllers.saveAndComeBack

import base.SpecBase
import forms.saveAndComeBack.ContinueRegistrationSelectionFormProvider
import models.saveAndComeBack.*
import models.{SavedUserAnswers, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.ContinueRegistrationSelectionPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.SaveAndComeBackService
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationSelectionView

class ContinueRegistrationSelectionControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSaveAndComeBackService = mock[SaveAndComeBackService]
  private val mockSessionRepository = mock[SessionRepository]

  private lazy val continueSelectionOnPageLoad: String = routes.ContinueRegistrationSelectionController.onPageLoad(waypoints).url
  private lazy val continueSelectionOnSubmitRoute: String = routes.ContinueRegistrationSelectionController.onSubmit(waypoints).url

  private val formProvider = new ContinueRegistrationSelectionFormProvider()
  private val form = formProvider()

  private val genericTaxReference: TaxReferenceInformation =
    TaxReferenceInformation(
      organisationName = "Organisation Name",
      taxReference = "tax reference",
      referenceNumber = "genericTaxNum",
      journeyId = "JourneyId"
    )

  private val seqTaxReference: Seq[TaxReferenceInformation] = Seq(genericTaxReference, genericTaxReference, genericTaxReference)

  private val testArbitraryUserAnswers: UserAnswers = arbitraryUserAnswers.arbitrary.sample.value
  private val testArbitrarySavedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value.copy(
    intermediaryNumber = intermediaryNumber
  )
  private val seqSavedUserAnswers: Seq[SavedUserAnswers] = Seq(testArbitrarySavedUserAnswers, testArbitrarySavedUserAnswers, testArbitrarySavedUserAnswers)
  private val singleRegistration: SingleRegistration = SingleRegistration(singleJourneyId = "journeyID")
  private val multipleRegistrations: MultipleRegistrations = MultipleRegistrations(multipleRegistrations = seqSavedUserAnswers)
  private val noRegistrations: NoRegistrations.type = NoRegistrations

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveAndComeBackService)
    Mockito.reset(mockSessionRepository)
  }

  "ContinueRegistrationSelectionController" - {

    ".onPageLoad" - {

      "when a single registration is returned from the service should set the user answers and redirect to continue registration page" in {

        val answers: UserAnswers = emptyUserAnswers.set(ContinueRegistrationSelectionPage, "journeyID").success.value

        when(mockSaveAndComeBackService.getSavedContinueRegistrationJourneys(any(), any())(any())) thenReturn singleRegistration.toFuture
        when(mockSaveAndComeBackService.retrieveSingleSavedUserAnswers(any(), any())(any(), any())) thenReturn testArbitraryUserAnswers.toFuture
        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {

          val request = FakeRequest(GET, continueSelectionOnPageLoad)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value mustEqual routes.ContinueRegistrationController.onPageLoad().url
          verify(mockSaveAndComeBackService, times(1)).getSavedContinueRegistrationJourneys(any(), any())(any())
          verify(mockSaveAndComeBackService, times(1)).retrieveSingleSavedUserAnswers(any(), any())(any(), any())
          verify(mockSessionRepository, times(1)).set(any())
        }
      }

      "when multiple registrations are returned from the service should display the multiple selection page with status OK" in {

        val answers: UserAnswers = emptyUserAnswers

        when(mockSaveAndComeBackService.getSavedContinueRegistrationJourneys(any(), any())(any())) thenReturn multipleRegistrations.toFuture
        when(mockSaveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(any())(any(), any())) thenReturn seqTaxReference.toFuture
        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {

          val request = FakeRequest(GET, continueSelectionOnPageLoad)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ContinueRegistrationSelectionView]

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(seqTaxReference, form, waypoints)(request, messages(application)).toString
          verify(mockSaveAndComeBackService, times(1)).getSavedContinueRegistrationJourneys(any(), any())(any())
          verify(mockSaveAndComeBackService, times(1)).createTaxReferenceInfoForSavedUserAnswers(any())(any(), any())
          verify(mockSessionRepository, times(1)).set(any())
        }
      }

      "when no registrations are returned from the service should direct user to the dashboard" in {

        val answers: UserAnswers = emptyUserAnswers

        when(mockSaveAndComeBackService.getSavedContinueRegistrationJourneys(any(), any())(any())) thenReturn noRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("urls.yourAccountUrl" -> "/dashboardUrl")
          .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
          .build()

        running(application) {

          val request = FakeRequest(GET, continueSelectionOnPageLoad)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` "/dashboardUrl"
          verify(mockSaveAndComeBackService, times(1)).getSavedContinueRegistrationJourneys(any(), any())(any())
        }
      }

      "must redirect to AccessDenied page" - {

        "when the journeyId does not match for a single registration" in {

          val registrationWithDifferentJourneyId = singleRegistration.copy(singleJourneyId = "invalid")

          val answers: UserAnswers = emptyUserAnswers.set(ContinueRegistrationSelectionPage, "journeyID").success.value

          when(mockSaveAndComeBackService.getSavedContinueRegistrationJourneys(any(), any())(any())) thenReturn registrationWithDifferentJourneyId.toFuture
          when(mockSaveAndComeBackService.retrieveSingleSavedUserAnswers(any(), any())(any(), any())) thenReturn testArbitraryUserAnswers.toFuture

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {

            val request = FakeRequest(GET, continueSelectionOnPageLoad)

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.AccessDeniedController.onPageLoad().url
            verify(mockSaveAndComeBackService, times(1)).getSavedContinueRegistrationJourneys(any(), any())(any())
          }
        }

        "when the intermediaryNumber does not match for multiple registrations" in {

          val savedUserAnswersWithDifferentIntermediaryNumber = testArbitrarySavedUserAnswers.copy(intermediaryNumber = "invalidIntNum")
          val multipleRegistrationsWithDifferentIntermediaryNumber =
            MultipleRegistrations(multipleRegistrations = Seq(savedUserAnswersWithDifferentIntermediaryNumber))

          val answers: UserAnswers = emptyUserAnswers

          when(mockSaveAndComeBackService.getSavedContinueRegistrationJourneys(any(), any())(any())) thenReturn
            multipleRegistrationsWithDifferentIntermediaryNumber.toFuture

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {

            val request = FakeRequest(GET, continueSelectionOnPageLoad)

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value `mustBe` controllers.routes.AccessDeniedController.onPageLoad().url
            verify(mockSaveAndComeBackService, times(1)).getSavedContinueRegistrationJourneys(any(), any())(any())
          }
        }
      }

    }

    ".onSubmit" - {

      "when a valid form is submitted" - {

        "should update the userAnswers and recursively call the ContinueRegistrationSelection controller" in {

          val answers: UserAnswers = emptyUserAnswers

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {

            val request = FakeRequest(POST, continueSelectionOnSubmitRoute)
              .withFormUrlEncodedBody(("value", testArbitrarySavedUserAnswers.journeyId))

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value mustBe ContinueRegistrationSelectionPage.route(waypoints).url
            verify(mockSessionRepository, times(1)).set(any())
          }
        }
      }

      "when a invalid form is submitted" - {

        "should draw the list of registrations from user answers and return bad request and errors" in {

          val answers: UserAnswers = emptyUserAnswers
            .set(ContinueRegistrationList, seqTaxReference).success.value

          val application = applicationBuilder(userAnswers = Some(answers))
            .build()

          running(application) {

            val request = FakeRequest(POST, continueSelectionOnSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val result = route(application, request).value

            val view1 = application.injector.instanceOf[ContinueRegistrationSelectionView]

            status(result) `mustBe` BAD_REQUEST
            contentAsString(result) `mustBe` view1(seqTaxReference, boundForm, waypoints)(request, messages(application)).toString
          }
        }

        "and an unexpected error has occurred storing the list of registrations should throw an exception [Edge Case]" in {
          val answers: UserAnswers = emptyUserAnswers

          val application = applicationBuilder(userAnswers = Some(answers))
            .build()

          running(application) {

            val request = FakeRequest(POST, continueSelectionOnSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Received an unexpected error as no registration list found"
            }
          }

        }

      }
    }
  }
}
