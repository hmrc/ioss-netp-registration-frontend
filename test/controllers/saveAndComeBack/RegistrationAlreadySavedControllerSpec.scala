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
import controllers.routes as normalRoutes
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import models.UserAnswers
import models.etmp.EtmpIdType
import models.etmp.EtmpIdType.{FTR, NINO, UTR, VRN}
import models.saveAndComeBack.ContinueRegistration
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessBasedInUKPage, SavedProgressPage}
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PreviousUnfinishedRegistration
import repositories.SessionRepository
import services.SaveAndComeBackService
import services.core.CoreSavedAnswersRevalidationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.RegistrationAlreadySavedView

import scala.concurrent.Future

class RegistrationAlreadySavedControllerSpec extends AnyFreeSpec with MockitoSugar with BeforeAndAfterEach with SpecBase with TableDrivenPropertyChecks {

  private val mockSaveAndComeBackService: SaveAndComeBackService = mock[SaveAndComeBackService]
  private val mockCoreSavedAnswersRevalidationService: CoreSavedAnswersRevalidationService = mock[CoreSavedAnswersRevalidationService]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  private lazy val RegistrationAlreadySavedOnPageLoadRoute: String = routes.RegistrationAlreadySavedController.onPageLoad(waypoints).url
  private lazy val RegistrationAlreadySavedOnSubmitRoute: String = routes.RegistrationAlreadySavedController.onSubmit(waypoints).url

  private val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

  private val formProvider = new ContinueRegistrationFormProvider

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveAndComeBackService)
    Mockito.reset(mockCoreSavedAnswersRevalidationService)
  }

  val previousUserAnswersWithAllRequiredFields: UserAnswers = emptyUserAnswers
    .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

  val userAnswersWithAllRequiredFields: UserAnswers = emptyUserAnswers
    .set(PreviousUnfinishedRegistration, previousUserAnswersWithAllRequiredFields).success.value
    .set(BusinessBasedInUKPage, true).success.value

  private val form = formProvider()


  "RegistrationAlreadySavedController" - {
    ".onPageLoad" - {
      "when User Answers has an unfinished registration journey, display the correct values in the view" in {

        val application = applicationBuilder(userAnswers = Some(userAnswersWithAllRequiredFields))
          .overrides(
            bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService),
          )
          .build()
        val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
        when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn

        running(application) {
          val request = FakeRequest(GET, RegistrationAlreadySavedOnPageLoadRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[RegistrationAlreadySavedView]
          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(form, waypoints, "MockCompanyName", "MockTaxRef")(request, messages(application)).toString
          verify(mockSaveAndComeBackService, times(1)).retrieveTaxRef(any())
        }
      }

      "when User Answers has an unfinished registration journey, but is missing PreviousUnfinishedRegistration throw error" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, RegistrationAlreadySavedOnPageLoadRoute)
          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp `mustBe` a[IllegalStateException]
            exp.getMessage `mustBe` "Must have previous unfinished registration journey"
          }
        }
      }

      "when User Answers has an unfinished registration journey, but is missing savedProgressUrl throw error" in {
        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers.set(PreviousUnfinishedRegistration, emptyUserAnswers).success.value))
          .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
          .build()

        val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
        when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn

        running(application) {
          val request = FakeRequest(GET, RegistrationAlreadySavedOnPageLoadRoute)
          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp `mustBe` a[IllegalStateException]
            exp.getMessage `mustBe` "Must have a saved page url to return to the saved journey"
          }
        }
      }
    }
    ".onSubmit" - {
      "when a valid form is submitted as continue" - {
        ",User Answers has all required values" - {

          "and checkAndValidateSavedUserAnswers returns SOME" +
            "delete the saved instance from the database and redirect to where the journey was last saved" in {

            val redirectResult: Result = Redirect(normalRoutes.ClientAlreadyRegisteredController.onPageLoad().url)

            val application = applicationBuilder(userAnswers = Some(userAnswersWithAllRequiredFields))
              .overrides(
                bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService),
                bind[CoreSavedAnswersRevalidationService].toInstance(mockCoreSavedAnswersRevalidationService)
              )
              .build()

            val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
            when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn
            when(mockCoreSavedAnswersRevalidationService.checkAndValidateSavedUserAnswers(any())(any(), any())) thenReturn Some(redirectResult).toFuture
            when(mockSaveAndComeBackService.deleteSavedUserAnswers(any())(any(), any())) thenReturn Future.successful(())

            running(application) {
              val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)
                .withFormUrlEncodedBody(("value", ContinueRegistration.Continue.toString))

              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value mustBe redirectLocation(redirectResult.toFuture).value
              verify(mockSaveAndComeBackService, times(1)).retrieveTaxRef(any())
              verify(mockCoreSavedAnswersRevalidationService, times(1)).checkAndValidateSavedUserAnswers(any())(any(), any())
            }
          }

          "and checkAndValidateSavedUserAnswers returns NONE " +
            "redirect to where the journey was last saved" in {

            val application = applicationBuilder(userAnswers = Some(userAnswersWithAllRequiredFields))
              .overrides(
                bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService),
                bind[CoreSavedAnswersRevalidationService].toInstance(mockCoreSavedAnswersRevalidationService)
              )
              .build()

            val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
            when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn
            when(mockCoreSavedAnswersRevalidationService.checkAndValidateSavedUserAnswers(any())(any(), any())) thenReturn None.toFuture

            running(application) {
              val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)
                .withFormUrlEncodedBody(("value", ContinueRegistration.Continue.toString))

              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              RedirectUrl(redirectLocation(result).value) mustBe continueUrl
              verify(mockSaveAndComeBackService, times(1)).retrieveTaxRef(any())
              verify(mockCoreSavedAnswersRevalidationService, times(1)).checkAndValidateSavedUserAnswers(any())(any(), any())
            }
          }
        }

        "and User Answers are missing a PreviousUnfinishedRegistration return error" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Must have previous unfinished registration journey"
            }

          }
        }

        "and User Answers are missing SavedProgressPage return error" in {
          val application = applicationBuilder(
            userAnswers = Some(emptyUserAnswers.set(PreviousUnfinishedRegistration, emptyUserAnswers).success.value))
            .overrides(
              bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService),
            )
            .build()

          val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
          when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn

          running(application) {
            val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)
              .withFormUrlEncodedBody(("value", ContinueRegistration.Continue.toString))

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "Illegal value submitted and/or must have a saved page url to return to the saved journey"
            }
          }
        }
      }

      "when a valid form is submitted as delete" - {
        "should remove PreviousUnfinishedRegistration, delete the saved instance and redirect based on idType" in {
          val testCases = Table(
            ("idType", "redirectURL"),
            (VRN, Redirect(controllers.routes.CheckVatDetailsController.onPageLoad().url)),
            (FTR, Redirect(controllers.routes.ClientBusinessNameController.onPageLoad().url)),
            (UTR, Redirect(controllers.routes.ClientBusinessAddressController.onPageLoad().url)),
            (NINO, Redirect(controllers.routes.ClientBusinessAddressController.onPageLoad().url))
          )

          val application = applicationBuilder(userAnswers = Some(userAnswersWithAllRequiredFields))
            .overrides(
              bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService),
              bind[CoreSavedAnswersRevalidationService].toInstance(mockCoreSavedAnswersRevalidationService)
            )
            .build()

          running(application) {

            forAll(testCases) { (idType, redirectURL) =>

              val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", idType)
              when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn
              when(mockSessionRepository.set(any())) thenReturn true.toFuture
              when(mockSaveAndComeBackService.deleteSavedUserAnswers(any())(any(), any())) thenReturn Future.successful(())

              val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)
                .withFormUrlEncodedBody(("value", ContinueRegistration.Delete.toString))

              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value mustBe redirectLocation(redirectURL.toFuture).value
            }
          }
        }
      }
      "when an invalid form is submitted" - {
        "return bad request and errors" in {
          val tupleReturn: Tuple3[String, String, EtmpIdType] = ("MockCompanyName", "MockTaxRef", VRN)
          when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn tupleReturn

          val application = applicationBuilder(userAnswers = Some(userAnswersWithAllRequiredFields))
            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
            .build()

          running(application) {

            val request = FakeRequest(POST, RegistrationAlreadySavedOnSubmitRoute)
              .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val result = route(application, request).value

            val view = application.injector.instanceOf[RegistrationAlreadySavedView]

            status(result) `mustBe` BAD_REQUEST
            contentAsString(result) mustBe view(boundForm, waypoints, "MockCompanyName", "MockTaxRef")(request, messages(application)).toString
            verify(mockSaveAndComeBackService, times(1)).retrieveTaxRef(any())
          }
        }
      }
    }
  }
}