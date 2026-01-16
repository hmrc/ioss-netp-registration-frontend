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

package controllers.amend

import base.SpecBase
import config.Constants.maxSchemes
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.domain.{PreviousRegistration, PreviousSchemeDetails}
import models.etmp.EtmpOtherAddress
import models.etmp.display.EtmpDisplayCustomerIdentification
import models.{BusinessContactDetails, ClientBusinessName, Country, InternationalAddress, TradingName, UserAnswers, Website}
import models.etmp.display.{EtmpDisplayRegistration, RegistrationWrapper}
import models.vatEuDetails.EuDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllWebsites, OriginalRegistrationQuery}
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.amend.AmendCompleteView
import utils.FutureSyntax.FutureOps

class AmendCompleteControllerSpec extends SpecBase with MockitoSugar {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  
  val userAnswers = UserAnswers(
    id = userAnswersId,
    data = Json.obj(
      BusinessContactDetailsPage.toString -> Json.obj(
        "fullName" -> "value 1",
        "telephoneNumber" -> "value 2",
        "emailAddress" -> "test@test.com",
        "websiteAddress" -> "value 4",
      )
    ),
    vatInfo = Some(vatCustomerInfo)
  )

  private val originalRegistration = userAnswers.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.etmpDisplayRegistration).success.value
  private val originalEtmpRegistration: EtmpDisplayRegistration = originalRegistration.get(OriginalRegistrationQuery(iossNumber)).get
  private val originalEtmpWithAddress = originalEtmpRegistration.copy(
    otherAddress = Some(EtmpOtherAddress(
      issuedBy = "FR",
      tradingName = Some("Client Name"),
      addressLine1 = "123 Street",
      addressLine2 = None,
      townOrCity = "Paris",
      regionOrState = None,
      postcode = Some("12345")
    ))
  )

  "AmendComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRegistrationConnector.displayIntermediaryRegistration(any())(any())) thenReturn Right(registrationWrapper).toFuture

      val application = applicationBuilder(userAnswers = Some(originalRegistration)).build()
      
      running(application) {
        val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]
        implicit val msgs: Messages = messages(application)
        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(userAnswers, Some(registrationWrapper)))

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          summaryList
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery and the correct view for a GET with no user answers" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
        val result = route(application, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are amended answers" - {

      "must show row for when trading name added" in {

        val newTradingName = TradingName("NewTradingName")
        val updatedAnswers = originalRegistration
          .set(AllTradingNamesQuery, List(newTradingName)).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]
          implicit val msgs: Messages = messages(application)
          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          contentAsString(result) mustBe view(
            config.feedbackUrl(request),
            config.intermediaryYourAccountUrl,
            summaryList
          )(request, messages(application)).toString

          val expectedRow = TradingNameSummary.amendedAnswersRow(updatedAnswers).get

          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when previous registration added" in {

        val previousRegistration: PreviousRegistration = PreviousRegistration(
          previousEuCountry = arbitraryCountry.arbitrary.sample.value,
          previousSchemesDetails = Gen.listOfN(maxSchemes, PreviousSchemeDetails(
            previousScheme = arbitraryPreviousScheme.arbitrary.sample.value,
            previousSchemeNumbers = arbitraryPreviousIossSchemeDetails.arbitrary.sample.value,
            nonCompliantDetails = Some(arbitraryNonCompliantDetails.arbitrary.sample.value)
          )).sample.value
        )
        val updatedAnswers = originalRegistration
          .set(AllPreviousRegistrationsQuery, List(previousRegistration)).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]
          implicit val msgs: Messages = messages(application)
          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          contentAsString(result) mustBe view(
            config.feedbackUrl(request),
            config.intermediaryYourAccountUrl,
            summaryList
          )(request, messages(application)).toString

          val expectedRow = PreviousRegistrationSummary.addedRow(updatedAnswers).get

          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when website added" in {

        val newWebsite = Website("https://www.NewWebsite.co.uk")
        val updatedAnswers = originalRegistration
          .set(AllWebsites, List(newWebsite)).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]
          implicit val msgs: Messages = messages(application)
          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          contentAsString(result) mustBe view(
            config.feedbackUrl(request),
            config.intermediaryYourAccountUrl,
            summaryList
          )(request, messages(application)).toString

          val expectedRow = WebsiteSummary.amendedAnswersRow(updatedAnswers).get

          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when contact details amended" in {

        val newBusinessContactDetails = BusinessContactDetails(
          fullName = "John Doe",
          telephoneNumber = "07912345678",
          emailAddress = "test@email.com"
        )

        val updatedAnswers = originalRegistration
          .set(BusinessContactDetailsPage, newBusinessContactDetails).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[AmendCompleteView]
          implicit val msgs: Messages = messages(application)
          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          contentAsString(result) mustBe view(
            config.feedbackUrl(request),
            config.intermediaryYourAccountUrl,
            summaryList
          )(request, messages(application)).toString

          val expectedContactNameRow = BusinessContactDetailsSummary.amendedRowContactName(updatedAnswers).get
          val expectedTelephoneRow = BusinessContactDetailsSummary.amendedRowTelephoneNumber(updatedAnswers).get
          val expectedEmail = BusinessContactDetailsSummary.amendedRowEmailAddress(updatedAnswers).get

          summaryList.rows must contain(expectedContactNameRow)
          summaryList.rows must contain(expectedTelephoneRow)
          summaryList.rows must contain(expectedEmail)
        }
      }

      "must show row for when country is amended and not UK" in {

        val userAnswersWithOriginal = originalRegistration
          .set(OriginalRegistrationQuery(iossNumber), originalEtmpWithAddress).success.value

        val amendedCountry = Country("DE", "Germany")
        val updatedAnswers = userAnswersWithOriginal.set(ClientCountryBasedPage, amendedCountry).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val result = route(application, request).value
          implicit val msgs: Messages = messages(application)

          status(result) mustBe OK

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          val expectedRow = ClientCountryBasedSummary.amendedRowWithoutAction(updatedAnswers).get
          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when trading name is amended" in {

        val userAnswersWithOriginal = originalRegistration
          .set(OriginalRegistrationQuery(iossNumber), originalEtmpWithAddress).success.value

        val amendedTradingName = ClientBusinessName("Amended trading Name")
        val updatedAnswers = userAnswersWithOriginal.set(ClientBusinessNamePage, amendedTradingName).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val result = route(application, request).value
          implicit val msgs: Messages = messages(application)

          status(result) mustBe OK

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          val expectedRow = ClientBusinessNameSummary.amendedRowWithoutAction(updatedAnswers).get
          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when otherAddress is amended" in {

        val userAnswersWithOriginal = originalRegistration
          .set(OriginalRegistrationQuery(iossNumber), originalEtmpWithAddress).success.value

        val amendedOtherAddress = InternationalAddress(
          line1 = "123 Street",
          line2 = None,
          townOrCity = "Paris",
          stateOrRegion = None,
          postCode = Some("12345"),
          country = None
        )
        val updatedAnswers = userAnswersWithOriginal.set(ClientBusinessAddressPage, amendedOtherAddress).success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val result = route(application, request).value
          implicit val msgs: Messages = messages(application)

          status(result) mustBe OK

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          val expectedRow = ClientBusinessAddressSummary.amendedRowWithoutAction(updatedAnswers).get
          summaryList.rows must contain(expectedRow)
        }
      }

      "must show row for when taxReference is amended" in {

        val userAnswersWithOriginal = originalRegistration
          .set(OriginalRegistrationQuery(iossNumber), originalEtmpWithAddress).success.value
        
        val updatedAnswers = userAnswersWithOriginal
          .set(ClientCountryBasedPage, Country("DE", "Germany")).success.value
          .set(ClientTaxReferencePage, "FTR_NUM_1").success.value

        val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.amend.routes.AmendCompleteController.onPageLoad(waypoints).url)
          val result = route(application, request).value
          implicit val msgs: Messages = messages(application)

          status(result) mustBe OK

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(registrationWrapper)))

          val expectedRow = ClientTaxReferenceSummary.amendedRowWithoutAction(updatedAnswers).get
          summaryList.rows must contain(expectedRow)
        }
      }
    }
  }

  private def getAmendedRegistrationSummaryList(
                                                 answers: UserAnswers,
                                                 registrationWrapper: Option[RegistrationWrapper]
                                               )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.amendedRow(answers)
    val tradingNameSummaryRow = TradingNameSummary.amendedAnswersRow(answers)
    val removedTradingNameRow = TradingNameSummary.removedAnswersRow(getRemovedTradingNames(answers, registrationWrapper))
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.amendedRow(answers)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.addedRow(answers)
    val hasFixedEstablishmentInEuDetails = HasFixedEstablishmentSummary.amendedRow(answers)
    val fixedEstablishmentInEuDetailsSummaryRow = EuDetailsSummary.addedRow(answers)
    val removeFixedEstablishmentInEuDetailsRow = EuDetailsSummary
      .removedRow(removedFixedEstablishmentInEuDetailsRow(answers, registrationWrapper.map(_.etmpDisplayRegistration)))
    val websiteSummaryRow = WebsiteSummary.amendedAnswersRow(answers)
    val removedWebsiteRow = WebsiteSummary.removedAnswersRow(getRemovedWebsites(answers, registrationWrapper))
    val businessContactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.amendedRowContactName(answers)
    val businessContactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.amendedRowTelephoneNumber(answers)
    val businessContactDetailsEmailSummaryRow = BusinessContactDetailsSummary.amendedRowEmailAddress(answers)
    val countryBasedRow = ClientCountryBasedSummary.amendedRowWithoutAction(answers)
    val principlePlaceOfBusinessRow = ClientBusinessAddressSummary.amendedRowWithoutAction(answers)
    val primaryTradingNameRow = ClientBusinessNameSummary.amendedRowWithoutAction(answers)
    val taxReferenceDetailsRow = ClientTaxReferenceSummary.amendedRowWithoutAction(answers)

    Seq(
      hasTradingNameSummaryRow,
      tradingNameSummaryRow,
      removedTradingNameRow,
      previouslyRegisteredSummaryRow,
      previousRegistrationSummaryRow,
      hasFixedEstablishmentInEuDetails,
      fixedEstablishmentInEuDetailsSummaryRow,
      removeFixedEstablishmentInEuDetailsRow,
      websiteSummaryRow,
      removedWebsiteRow,
      businessContactDetailsContactNameSummaryRow,
      businessContactDetailsTelephoneSummaryRow,
      businessContactDetailsEmailSummaryRow,
      countryBasedRow,
      principlePlaceOfBusinessRow,
      primaryTradingNameRow,
      taxReferenceDetailsRow
    ).flatten
  }

  private def getRemovedTradingNames(answers: UserAnswers, registrationWrapper: Option[RegistrationWrapper]): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNamesQuery).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.map(_.etmpDisplayRegistration.tradingNames.map(_.tradingName)).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)

  }

  private def getRemovedWebsites(answers: UserAnswers, registrationWrapper: Option[RegistrationWrapper]): Seq[String] = {

    val amendedAnswers = answers.get(AllWebsites).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.map(_.etmpDisplayRegistration.schemeDetails.websites.map(_.websiteAddress)).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)
  }

  private def removedFixedEstablishmentInEuDetailsRow(answers: UserAnswers, etmpDisplayRegistration: Option[EtmpDisplayRegistration]): Seq[Country] = {

    val amendedAnswers = answers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(List.empty)
    val originalAnswers = etmpDisplayRegistration.map(_.schemeDetails.euRegistrationDetails.map(_.issuedBy)).getOrElse(Seq.empty)

    val removedCountries = originalAnswers.diff(amendedAnswers)

    removedCountries.flatMap(Country.fromCountryCode)
  }
}
