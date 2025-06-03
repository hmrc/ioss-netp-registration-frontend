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
import forms.CheckVatDetailsFormProvider
import models.checkVatDetails.CheckVatDetails
import models.{ClientBusinessName, Country, InternationalAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ConfirmClientVatDetailsView

import scala.concurrent.Future

class CheckVatDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints
  private val utr: String = "1234567890"
  private val nino = "QQ 12 34 56 C"
  private val companyName: String = "Company name"
  private val countries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(countries).sample.value
  private val taxReference: String = "123456789"
  private val businessAddress: InternationalAddress = InternationalAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    stateOrRegion = None,
    postCode = None,
    country = Some(country)
  )

  val formProvider = new CheckVatDetailsFormProvider
  val form: Form[CheckVatDetails] = formProvider()

  private val checkVatDetailsPage = CheckVatDetailsPage()
  private val updatedAnswersNonUK = emptyUserAnswers
    .set(BusinessBasedInUKPage, false).success.value
    .set(ClientCountryBasedPage, country).success.value
    .set(ClientBusinessNamePage, ClientBusinessName(companyName)).success.value
    .set(ClientTaxReferencePage, taxReference).success.value
    .set(ClientBusinessAddressPage, businessAddress).success.value

  private val updatedAnswersUtr = emptyUserAnswers
    .set(BusinessBasedInUKPage, true).success.value
    .set(ClientHasVatNumberPage, false).success.value
    .set(ClientBusinessNamePage, ClientBusinessName(companyName)).success.value
    .set(ClientHasUtrNumberPage, true).success.value
    .set(ClientUtrNumberPage, utr).success.value
    .set(ClientBusinessAddressPage, businessAddress).success.value

  private val updatedAnswersNino = emptyUserAnswers
    .set(BusinessBasedInUKPage, true).success.value
    .set(ClientHasVatNumberPage, false).success.value
    .set(ClientBusinessNamePage, ClientBusinessName(companyName)).success.value
    .set(ClientHasUtrNumberPage, false).success.value
    .set(ClientsNinoNumberPage, nino).success.value
    .set(ClientBusinessAddressPage, businessAddress).success.value

  lazy val checkVatDetailsRoute: String = routes.CheckVatDetailsController.onPageLoad(waypoints).url

  "CheckVatDetails Controller" - {

    "when Business Based in the UK is False" - {
      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(updatedAnswersNonUK)).build()

        running(application) {

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, checkVatDetailsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ConfirmClientVatDetailsView]

          val summaryList: SummaryList = SummaryListViewModel(
            rows = Seq(
              BusinessBasedInUKSummary.row(waypoints, updatedAnswersNonUK, checkVatDetailsPage),
              ClientBusinessNameSummary.row(waypoints, updatedAnswersNonUK, checkVatDetailsPage),
              ClientCountryBasedSummary.row(waypoints, updatedAnswersNonUK, checkVatDetailsPage),
              ClientTaxReferenceSummary.row(waypoints, updatedAnswersNonUK, checkVatDetailsPage),
              ClientBusinessAddressSummary.row(waypoints, updatedAnswersNonUK, checkVatDetailsPage)
            ).flatten
          )

          status(result) `mustBe` OK
          contentAsString(result) mustBe view(waypoints, summaryList, companyName)(request, messages(application)).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(updatedAnswersNonUK))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, checkVatDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual CheckVatDetailsPage().navigate(waypoints, updatedAnswersNonUK, updatedAnswersNonUK).url
        }
      }


      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, checkVatDetailsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, checkVatDetailsRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }

    "when Business Based in the UK is True" - {

      "and the client doesn't have a UK vat number" - {

        "the client has a UTR" - {

          "must return OK and the correct view for a GET" in {

            val application = applicationBuilder(userAnswers = Some(updatedAnswersUtr)).build()

            running(application) {

              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, checkVatDetailsRoute)

              val result = route(application, request).value

              val view = application.injector.instanceOf[ConfirmClientVatDetailsView]

              val summaryList: SummaryList = SummaryListViewModel(
                rows = Seq(
                  BusinessBasedInUKSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage),
                  ClientHasVatNumberSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage),
                  ClientBusinessNameSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage),
                  ClientHasUtrNumberSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage),
                  ClientUtrNumberSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage),
                  ClientBusinessAddressSummary.row(waypoints, updatedAnswersUtr, checkVatDetailsPage)
                ).flatten
              )

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(waypoints, summaryList, companyName)(request, messages(application)).toString
            }
          }

          "must redirect to the next page when valid data is submitted" in {

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(updatedAnswersUtr))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, checkVatDetailsRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual CheckVatDetailsPage().navigate(waypoints, updatedAnswersUtr, updatedAnswersUtr).url
            }
          }


          "must redirect to Journey Recovery for a GET if no existing data is found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request = FakeRequest(GET, checkVatDetailsRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "must redirect to Journey Recovery for a POST if no existing data is found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request =
                FakeRequest(POST, checkVatDetailsRoute)
                  .withFormUrlEncodedBody(("value", "answer"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

        "the client has a NINO" - {
          "must return OK and the correct view for a GET" in {

            val application = applicationBuilder(userAnswers = Some(updatedAnswersNino)).build()

            running(application) {

              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, checkVatDetailsRoute)

              val result = route(application, request).value

              val view = application.injector.instanceOf[ConfirmClientVatDetailsView]

              val summaryList: SummaryList = SummaryListViewModel(
                rows = Seq(
                  BusinessBasedInUKSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage),
                  ClientHasVatNumberSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage),
                  ClientBusinessNameSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage),
                  ClientHasUtrNumberSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage),
                  ClientsNinoNumberSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage),
                  ClientBusinessAddressSummary.row(waypoints, updatedAnswersNino, checkVatDetailsPage)
                ).flatten
              )

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(waypoints, summaryList, companyName)(request, messages(application)).toString
            }
          }

          "must redirect to the next page when valid data is submitted" in {

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(updatedAnswersNino))
                .overrides(
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, checkVatDetailsRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual CheckVatDetailsPage().navigate(waypoints, updatedAnswersNino, updatedAnswersNino).url
            }
          }


          "must redirect to Journey Recovery for a GET if no existing data is found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request = FakeRequest(GET, checkVatDetailsRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "must redirect to Journey Recovery for a POST if no existing data is found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request =
                FakeRequest(POST, checkVatDetailsRoute)
                  .withFormUrlEncodedBody(("value", "answer"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }

      }
    }
  }
}
