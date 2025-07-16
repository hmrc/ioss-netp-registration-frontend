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

package utils

import base.SpecBase
import models.domain.PreviousSchemeNumbers
import models.previousRegistrations.{PreviousRegistrationDetailsWithOptionalVatNumber, SchemeDetailsWithOptionalVatNumber, SchemeNumbersWithOptionalVatNumber}
import models.requests.DataRequest
import models.{Country, Index, PreviousScheme, PreviousSchemeType, UserAnswers}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.previousRegistrations.*
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.*
import queries.previousRegistrations.AllPreviousRegistrationsWithOptionalVatNumberQuery

class PreviousRegistrationsCompletionChecksSpec extends SpecBase with MockitoSugar {

  private val PreviousRegistrationsCompletionChecksTests: PreviousRegistrationsCompletionChecks.type = {
    PreviousRegistrationsCompletionChecks
  }

  private val countryIndex = Index(0)
  private val schemeIndex = Index(0)
  private val countries: Seq[Country] = Gen.listOf(arbitraryCountry.arbitrary).sample.value
  private val country: Country = Gen.oneOf(countries).sample.value

  private val schemeDetails = SchemeDetailsWithOptionalVatNumber(
    previousScheme = Some(PreviousScheme.IOSSWOI),
    clientHasIntermediary = Some(false),
    previousSchemeNumbers = Some(
      SchemeNumbersWithOptionalVatNumber(Some("IM0401234567")))
  )

  private val registrationDetails = PreviousRegistrationDetailsWithOptionalVatNumber(
    previousEuCountry = country,
    previousSchemesDetails = Some(List(schemeDetails))
  )

  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(PreviouslyRegisteredPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex), country).success.value
    .set(PreviousSchemeTypePage(countryIndex, schemeIndex), PreviousSchemeType.IOSS).success.value
    .set(ClientHasIntermediaryPage(countryIndex, schemeIndex), false).success.value

  ".isPreviouslyRegisteredDefined" - {

    "when the PreviouslyRegisteredPage question is Yes" - {

      "must return true when answers for the section are defined" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = PreviousRegistrationsCompletionChecksTests.isPreviouslyRegisteredDefined()

          result mustBe true
        }
      }

      "must return false when answers for the section are absent" in {

        val emptyAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(PreviouslyRegisteredPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(emptyAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn emptyAnswers

          val result = PreviousRegistrationsCompletionChecksTests.isPreviouslyRegisteredDefined()

          result mustBe false
        }
      }
    }

    "when the PreviouslyRegisteredPage question is No" - {

      "must return true when answers for the section are empty" in {

        val emptyAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(PreviouslyRegisteredPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(emptyAnswers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn emptyAnswers

          val result = PreviousRegistrationsCompletionChecksTests.isPreviouslyRegisteredDefined()

          result mustBe true
        }

      }

      "must return false when answers for the section are defined" in {

        val answers: UserAnswers = validAnswers
          .set(PreviouslyRegisteredPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {

          implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
          when(request.userAnswers) thenReturn answers

          val result = PreviousRegistrationsCompletionChecksTests.isPreviouslyRegisteredDefined()

          result mustBe false
        }

      }

    }
  }

  ".incompletePreviousRegistrationRedirect" - {

    "must redirect to the correct page when there is no Number present" in {

      val invalidAnswers: UserAnswers = validAnswers

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.incompletePreviousRegistrationRedirect(waypoints)

        result `mustBe` Some(Redirect(PreviousIossNumberPage(countryIndex, schemeIndex).route(waypoints).url))
      }

    }

    "must redirect to the correct page when there is no answer to Client Has Intermediary present" in {

      val invalidAnswers: UserAnswers = validAnswers
        .remove(ClientHasIntermediaryPage(countryIndex, schemeIndex)).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.incompletePreviousRegistrationRedirect(waypoints)

        result `mustBe` Some(Redirect(ClientHasIntermediaryPage(countryIndex, schemeIndex).route(waypoints).url))
      }
    }

    "must return None when a valid Number is present" in {

      val invalidAnswers: UserAnswers = validAnswers
        .set(AllPreviousRegistrationsWithOptionalVatNumberQuery, List(registrationDetails)).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.incompletePreviousRegistrationRedirect(waypoints)

        result `mustBe` None
      }
    }
  }

  ".getAllIncompleteRegistrationDetails" - {

    "must return a Seq of incomplete Previous Intermediary Registrations when Intermediary Numbers are missing" in {

      val invalidAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(PreviouslyRegisteredPage, true).success.value
        .set(PreviousEuCountryPage(countryIndex), country).success.value
        .set(PreviousSchemeTypePage(countryIndex, schemeIndex), PreviousSchemeType.IOSS).success.value
        .set(PreviousSchemePage(countryIndex, schemeIndex), PreviousScheme.IOSSWOI).success.value
        .set(ClientHasIntermediaryPage(countryIndex, schemeIndex), false).success.value

      val expected = List(
        PreviousRegistrationDetailsWithOptionalVatNumber(
          previousEuCountry = country,
          previousSchemesDetails = Some(List(
            SchemeDetailsWithOptionalVatNumber(
              previousScheme = Some(PreviousScheme.IOSSWOI),
              clientHasIntermediary = Some(false),
              previousSchemeNumbers = None
            )
          ))
        )
      )

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.getAllIncompleteRegistrationDetails()

        result `mustBe` expected

      }
    }

    "must return a Seq of incomplete Previous Intermediary Registrations when Client has Intermediary answer is missing" in {

      val invalidAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(PreviouslyRegisteredPage, true).success.value
        .set(PreviousEuCountryPage(countryIndex), country).success.value
        .set(PreviousSchemeTypePage(countryIndex, schemeIndex), PreviousSchemeType.IOSS).success.value
        .set(PreviousSchemePage(countryIndex, schemeIndex), PreviousScheme.IOSSWOI).success.value
        .set(PreviousIossNumberPage(Index(0), Index(0)), PreviousSchemeNumbers("123456789")).success.value

      val expected = List(
        PreviousRegistrationDetailsWithOptionalVatNumber(
          previousEuCountry = country,
          previousSchemesDetails = Some(List(
            SchemeDetailsWithOptionalVatNumber(
              previousScheme = Some(PreviousScheme.IOSSWOI),
              clientHasIntermediary = None,
              previousSchemeNumbers = Some(SchemeNumbersWithOptionalVatNumber(Some("123456789")))
            )
          ))
        )
      )

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.getAllIncompleteRegistrationDetails()

        result `mustBe` expected

      }
    }

    "must return List.empty when there are no incomplete Previous Intermediary Registration entries present" in {

      val completeAnswers = validAnswers
        .set(AllPreviousRegistrationsWithOptionalVatNumberQuery, List(registrationDetails)).success.value

      val application = applicationBuilder(userAnswers = Some(completeAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn completeAnswers

        val result = PreviousRegistrationsCompletionChecksTests.getAllIncompleteRegistrationDetails()

        result `mustBe` List.empty
      }
    }
  }

  ".emptyPreviousRegistrationRedirect" - {

    "must redirect to the correct page when Previous Registrations answers are expected but none are present" in {

      val invalidAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(PreviouslyRegisteredPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.emptyPreviousRegistrationRedirect(waypoints)

        result `mustBe` Some(Redirect(PreviouslyRegisteredPage.route(waypoints).url))
      }
    }

    "must redirect to the correct page when Previous Registrations answers are not expected but are present" in {

      val invalidAnswers: UserAnswers = validAnswers
        .set(PreviouslyRegisteredPage, false).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = PreviousRegistrationsCompletionChecksTests.emptyPreviousRegistrationRedirect(waypoints)

        result `mustBe` Some(Redirect(PreviouslyRegisteredPage.route(waypoints).url))
      }
    }

    "must return None when Previous Registrations answers are expected and are present" in {

      val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

      running(application) {
        implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
        when(request.userAnswers) thenReturn validAnswers

        val result = PreviousRegistrationsCompletionChecksTests.emptyPreviousRegistrationRedirect(waypoints)

        result `mustBe` None
      }
    }
  }
}
