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
import controllers.clientDeclarationJourney
import models.responses.NotFound
import models.{BusinessContactDetails, ClientBusinessName, IntermediaryInformation, SavedPendingRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, EmptyWaypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.IntermediaryStuffQuery
import utils.FutureSyntax.FutureOps

class ClientJourneyStartControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {


  def generate6DigitCode(): String = {
    util.Random.alphanumeric.filter(_.isUpper).take(6).mkString
  }

  private val businessContactDetails: BusinessContactDetails = arbitraryBusinessContactDetails.arbitrary.sample.value
  private val nonEmptyIntermediaryName: String = intermediaryVatCustomerInfo.organisationName.getOrElse("Intermediary Company Name2")

  val incompleteUserAnswers: UserAnswers =
    emptyUserAnswers.set(ClientBusinessNamePage, ClientBusinessName("Client Company"))
      .success.value
      .set(IntermediaryStuffQuery, IntermediaryInformation(intermediaryNumber, nonEmptyIntermediaryName))
      .success.value

  val completeUserAnswers: UserAnswers = incompleteUserAnswers.set(BusinessContactDetailsPage, businessContactDetails).success.value

  private def savedPendingRegistration(userAnswers: UserAnswers): SavedPendingRegistration =
    SavedPendingRegistration(
      journeyId = incompleteUserAnswers.journeyId,
      uniqueUrlCode = generate6DigitCode(),
      userAnswers = userAnswers,
      lastUpdated = incompleteUserAnswers.lastUpdated,
      uniqueActivationCode = generate6DigitCode(),
      intermediaryStuff = IntermediaryInformation(intermediaryNumber, nonEmptyIntermediaryName)
    )

  private def clientJourneyStartOnPageLoad(uniqueUrlCode: String): String = clientDeclarationJourney.routes.ClientJourneyStartController.onPageLoad(waypoints, uniqueUrlCode).url

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = reset(mockRegistrationConnector)

  "ClientJourneyStartController" - {

    ".onPageLoad()- GET must" - {

      "return redirect to the clientCodeEntryController with enriched userAnswers with valid url" in {

        val testSavedPendingRegistration: SavedPendingRegistration = savedPendingRegistration(completeUserAnswers)
        val testUniqueUrl = testSavedPendingRegistration.uniqueUrlCode

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(testSavedPendingRegistration).toFuture)

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, clientDeclarationJourney.routes.ClientJourneyStartController.onPageLoad(waypoints, testUniqueUrl).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(EmptyWaypoints, uniqueUrlCode = testUniqueUrl).url
          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrl))(any())
        }
      }

      "return redirect to the clientCodeEntryController when userAnswers have already been enriched" in {

        val testSavedPendingRegistration = savedPendingRegistration(completeUserAnswers)
        val testUniqueUrl = testSavedPendingRegistration.uniqueUrlCode

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(testSavedPendingRegistration).toFuture)

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, clientJourneyStartOnPageLoad(testUniqueUrl))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual clientDeclarationJourney.routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode = testUniqueUrl).url
          verify(mockRegistrationConnector, times(1)).getPendingRegistration(eqTo(testUniqueUrl))(any())
        }
      }

      "return error when userAnswers is missing client email" in {

        //SCG TODO- Can we handle the Error better rather than throwing IllegalStateException
        val testSavedPendingRegistration = savedPendingRegistration(incompleteUserAnswers)
        val testUniqueUrl = testSavedPendingRegistration.uniqueUrlCode

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Right(testSavedPendingRegistration).toFuture)

        val application = applicationBuilder(userAnswers = Some(incompleteUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, clientJourneyStartOnPageLoad(testUniqueUrl))

          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp `mustBe` a[Exception]
            exp.getMessage `mustBe` exp.getLocalizedMessage
          }

        }
      }

      "return error when uniqueUrlCode is not valid" in {

        when(mockRegistrationConnector.getPendingRegistration(any())(any()))
          .thenReturn(Left(NotFound).toFuture)

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, clientJourneyStartOnPageLoad("InvalidCode"))

          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp `mustBe` a[Exception]
            exp.getMessage `mustBe` exp.getLocalizedMessage
          }
        }
      }
    }

  }
}
