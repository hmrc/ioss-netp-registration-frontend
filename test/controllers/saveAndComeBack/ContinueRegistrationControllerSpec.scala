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

package controllers.saveAndComeBack

import base.SpecBase
import connectors.SaveForLaterConnector
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import models.saveAndComeBack.ContinueRegistration.{Continue, Delete}
import models.UserAnswers
import models.domain.VatCustomerInfo
import models.saveAndComeBack.TaxReferenceInformation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.*
import org.scalatestplus.mockito.MockitoSugar
import pages.{ClientVatNumberPage, SavedProgressPage, UkVatNumberNotFoundPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.SaveAndComeBackService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationView

class ContinueRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

  private val mockSaveAndComeBackService = mock[SaveAndComeBackService]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockSaveForLaterConnector = mock[SaveForLaterConnector]

  private lazy val continueOnPageLoadRoute: String = routes.ContinueRegistrationController.onPageLoad(waypoints).url
  private lazy val continueOnSubmitRoute: String = routes.ContinueRegistrationController.onSubmit(waypoints).url

  private val formProvider = new ContinueRegistrationFormProvider()
  private val form = formProvider()

  private val genericTaxReference: TaxReferenceInformation =
    TaxReferenceInformation(
      organisationName = "Organisation Name",
      taxReference = "tax reference",
      referenceNumber = "genericTaxNum",
      journeyId = "JourneyId"
    )

  private val vatTaxReference: TaxReferenceInformation =
    TaxReferenceInformation(
      organisationName = "Organisation Name",
      taxReference = "vat reference",
      referenceNumber = "genericTaxNum",
      journeyId = "JourneyId"
    )

  private val genericVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveAndComeBackService)
    Mockito.reset(mockSessionRepository)
    Mockito.reset(mockSaveForLaterConnector)
  }

  "ContinueRegistrationController" - {
    ".onPageLoad" - {
      "when the client has a UK VAT tax reference" - {

        "should retrieve the tax information, update userAnswers and display the correct view with status OK" in {

          val answers: UserAnswers = emptyUserAnswers
            .set(ClientVatNumberPage, "SomeVatNum").success.value
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          when(mockSaveAndComeBackService.getVatTaxInfo(any(), any())(any(), any())) thenReturn Right(genericVatInfo).toFuture
          when(mockSessionRepository.set(any())) thenReturn true.toFuture
          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn vatTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {

            val request = FakeRequest(GET, continueOnPageLoadRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[ContinueRegistrationView]

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(vatTaxReference, form, waypoints)(request, messages(application)).toString
            verify(mockSaveAndComeBackService, times(1)).getVatTaxInfo(any(), any())(any(), any())
            verify(mockSessionRepository, times(1)).set(any())
            verify(mockSaveAndComeBackService, times(1)).determineTaxReference(any())
          }
        }

        "and retrieving the vat reference fails should display the relevant error view" in {
          //TODO- SCG1
          val answers: UserAnswers = emptyUserAnswers
            .set(ClientVatNumberPage, "SomeVatNum").success.value
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          val expectedRedirectUrl = UkVatNumberNotFoundPage.route(waypoints).url
          val expectedRedirectCall = Call(GET, expectedRedirectUrl)

          when(mockSaveAndComeBackService.getVatTaxInfo(any(), any())(any(), any())) thenReturn Left(expectedRedirectCall).toFuture

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {

            val request = FakeRequest(GET, continueOnPageLoadRoute)

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value `mustBe` expectedRedirectUrl
            verify(mockSaveAndComeBackService, times(1)).getVatTaxInfo(any(), any())(any(), any())
          }
        }

        "but saved progress page was not saved properly should throw an exception" in {
          //TODO- SCG2
          val answers: UserAnswers = emptyUserAnswers
            .set(ClientVatNumberPage, "SomeVatNum").success.value


          when(mockSaveAndComeBackService.getVatTaxInfo(any(), any())(any(), any())) thenReturn Right(genericVatInfo).toFuture
          when(mockSessionRepository.set(any())) thenReturn true.toFuture
          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn vatTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {
            val request = FakeRequest(GET, continueOnPageLoadRoute)

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Must have a saved page url to return to the saved journey"
            }
          }
        }
      }

      "when the client has any other tax reference" - {

        "should display the correct view for the reference with status OK" in {

          val answers: UserAnswers = emptyUserAnswers
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {

            val request = FakeRequest(GET, continueOnPageLoadRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[ContinueRegistrationView]

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(genericTaxReference, form, waypoints)(request, messages(application)).toString
            verify(mockSaveAndComeBackService, times(1)).determineTaxReference(any())
          }
        }

        "but saved progress page was not saved properly should throw an exception" in {
          //TODO- SCG2
          val answers: UserAnswers = emptyUserAnswers

          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {
            val request = FakeRequest(GET, continueOnPageLoadRoute)

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Must have a saved page url to return to the saved journey"
            }
          }
        }
      }
    }

    ".onSubmit" - {
      "when a valid form is submitted" - {

        "and the value is continue, should get the continue url and direct to the correct view" in {

          val answers: UserAnswers = emptyUserAnswers
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {

            val request = FakeRequest(POST, continueOnSubmitRoute)
              .withFormUrlEncodedBody(("value", Continue.toString))

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            RedirectUrl(redirectLocation(result).value) mustEqual continueUrl
            verify(mockSaveAndComeBackService, times(1)).determineTaxReference(any())
          }
        }

        "and the value is delete, should clear the userAnswers, call connector to delete saved journey, and redirect to the dashboard" in {
          // TODO - SCG3
          val answers: UserAnswers = emptyUserAnswers
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference
          when(mockSessionRepository.clear(any())) thenReturn true.toFuture
          when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Right(true).toFuture

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
            .build()

          running(application) {

            val request = FakeRequest(POST, continueOnSubmitRoute)
              .withFormUrlEncodedBody(("value", Delete.toString))

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad().url
            // TODO - SCG3 -> should redirect to dashboard
            verify(mockSaveAndComeBackService, times(1)).determineTaxReference(any())
            verify(mockSessionRepository, times(1)).clear(any())
            verify(mockSaveForLaterConnector, times(1)).delete(any())(any())
          }
        }

        "but the saved progress page url is not present should throw an exception" in {

          val answers: UserAnswers = emptyUserAnswers

          when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {
            val request = FakeRequest(POST, continueOnSubmitRoute)
              .withFormUrlEncodedBody(("value", Continue.toString))

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Illegal value submitted and/or must have a saved page url to return to the saved journey"
            }
          }
        }

        "when a invalid form is submitted" - {

          "must return a Bad Request and errors when invalid data is submitted" in {

            val answers: UserAnswers = emptyUserAnswers
              .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

            when(mockSaveAndComeBackService.determineTaxReference(any())) thenReturn genericTaxReference

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
              .build()

            running(application) {

              val request = FakeRequest(POST, continueOnSubmitRoute)
                .withFormUrlEncodedBody(("value", ""))

              val boundForm = form.bind(Map("value" -> ""))

              val result = route(application, request).value

              val view = application.injector.instanceOf[ContinueRegistrationView]

              status(result) `mustBe` BAD_REQUEST
              contentAsString(result) `mustBe` view(genericTaxReference, boundForm, waypoints)(request, messages(application)).toString
              verify(mockSaveAndComeBackService, times(1)).determineTaxReference(any())
            }
          }
        }
      }
    }
  }
}
