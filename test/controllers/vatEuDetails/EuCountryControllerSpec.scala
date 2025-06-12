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

package controllers.vatEuDetails

import base.SpecBase
import forms.vatEuDetails.EuCountryFormProvider
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, Waypoints}
import pages.vatEuDetails.{EuCountryPage, VatRegisteredInEuPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.vatEuDetails.EuCountryView

class EuCountryControllerSpec extends SpecBase with MockitoSugar {

  private val euCountries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(euCountries).sample.value

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(VatRegisteredInEuPage, true).success.value

  private val formProvider = new EuCountryFormProvider()
  private val form: Form[Country] = formProvider(countryIndex(0), euCountries)

  private lazy val euCountryRoute: String = routes.EuCountryController.onPageLoad(waypoints, countryIndex(0)).url

  "EuCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuCountryView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex(0))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(EuCountryPage(countryIndex(0)), country).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(country), waypoints, countryIndex(0))(request, messages(application)).toString
      }
    }


  }
}