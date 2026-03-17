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
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import models.UserAnswers
import models.etmp.EtmpIdType
import models.etmp.EtmpIdType.VRN
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.PreviousUnfinishedRegistration
import services.SaveAndComeBackService
import services.core.CoreSavedAnswersRevalidationService
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.RegistrationAlreadySavedView

class RegistrationAlreadySavedControllerSpec extends AnyFreeSpec with MockitoSugar with BeforeAndAfterEach with SpecBase {
  // formProvider: ContinueRegistrationFormProvider
  //view: RegistrationAlreadySavedView
  val mockSaveAndComeBackService: SaveAndComeBackService = mock[SaveAndComeBackService]
  val mockCoreSavedAnswersRevalidationService: CoreSavedAnswersRevalidationService = mock[CoreSavedAnswersRevalidationService]
  val RegistrationAlreadySavedOnPageLoadRoute: String = routes.RegistrationAlreadySavedController.onPageLoad(waypoints).url
  val RegistrationAlreadySavedOnSubmitRoute: String = routes.RegistrationAlreadySavedController.onSubmit(waypoints).url

  private val formProvider = new ContinueRegistrationFormProvider

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveAndComeBackService)
    Mockito.reset(mockCoreSavedAnswersRevalidationService)
  }

  val previousUserAnswersWithAllRequiredFields: UserAnswers = emptyUserAnswers
    .set(SavedProgressPage, "continueUrl").success.value

  val userAnswersWithAllRequiredFields: UserAnswers = emptyUserAnswers
    .set(PreviousUnfinishedRegistration, previousUserAnswersWithAllRequiredFields).success.value

  private val form = formProvider()

  //          val application = applicationBuilder(userAnswers = ???)
  //            .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
  //            .build()


  "RegistrationAlreadySavedController" - {
    ".onPageLoad" - {
      "when User Answers has an unfinished registration journey, display the correct values in the view" in {
        
        val application = applicationBuilder(userAnswers = Some(previousUserAnswersWithAllRequiredFields))
          .overrides(bind[SaveAndComeBackService].toInstance(mockSaveAndComeBackService))
          .build()
        val theReturn: Tuple3[String, String, EtmpIdType] = ("MockResult", "MockResult", VRN)
        when(mockSaveAndComeBackService.retrieveTaxRef(any())) thenReturn theReturn

        running(application) {
          val request = FakeRequest(GET, RegistrationAlreadySavedOnPageLoadRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[RegistrationAlreadySavedView]
          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(form, waypoints, "MockCompanyName", "MockTaxRef")(request, messages(application)).toString
          verify(mockSaveAndComeBackService, times(1)).retrieveTaxRef(any())
        }
      }
      "when User Answers has an unfinished registration journey, but is missing some required data throw error" in {
        pending
      }
      "when User Answers has an unfinished registration journey, but is missing savedProgressUrl throw error" in {
        pending
      }
      "when User Answers is missing a unfinished registration journey throw error" in {
        pending
      }
    }
    ".onSubmit" - {
      "when a valid form is submitted as continue" - {
        ",User Answers has all required values" - {
          "and checkAndValidateSavedUserAnswers returns SOME" +
            "delete the saved instance from the database and redirect to where the journey was last saved" in {
            pending
          }
          "and checkAndValidateSavedUserAnswers returns NONE " +
            "redirect to where the journey was last saved" in {
            pending
          }
        }

        "and User Answers are missing a PreviousUnfinishedRegistration return error" in {
          pending
        }
        "and User Answers are missing SavedProgressPage return error" in {
          pending
        }
      }
      "when a valid form is submitted as delete" - {
        "should remove PreviousUnfinishedRegistration, delete the saved instance and redirect based on idType" in {
          pending
        }
      }
      "when an invalid form is submitted" - {
        "return bad request and errors" in {
          pending
          //          running(application) {
          //
          //            val request = FakeRequest(POST, continueSelectionOnSubmitRoute)
          //              .withFormUrlEncodedBody(("value", ""))
          //
          //            val boundForm = form.bind(Map("value" -> ""))
          //
          //            val result = route(application, request).value
          //
          //            val view1 = application.injector.instanceOf[ContinueRegistrationSelectionView]
          //
          //            status(result) `mustBe` BAD_REQUEST
          //            contentAsString(result) `mustBe` view1(seqTaxReference, boundForm, waypoints)(request, messages(application)).toString
          //          }
        }
      }
    }
  }
}
