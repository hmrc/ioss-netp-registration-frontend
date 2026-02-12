package services.core

import base.SpecBase
import controllers.routes
import models.PreviousScheme.{IOSSWI, OSSNU, OSSU}
import models.PreviousSchemeType.{IOSS, OSS}
import models.core.{Match, TraderId}
import models.domain.{PreviousRegistration, PreviousSchemeDetails, VatCustomerInfo}
import models.ossRegistration.ExclusionReason
import models.requests.DataRequest
import models.vatEuDetails.RegistrationType.{TaxId, VatNumber}
import models.vatEuDetails.{EuDetails, RegistrationType, TradingNameAndBusinessAddress}
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.previousRegistrations.*
import pages.vatEuDetails.*
import pages.{ClientCountryBasedPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreSavedAnswersRevalidationServiceSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, emptyUserAnswersWithVatInfo, intermediaryNumber, None)

  implicit private val dataRequest: DataRequest[AnyContent] =
    DataRequest(request, vrn.vrn, emptyUserAnswersWithVatInfo, intermediaryNumber, None)

  private val intermediaryNumber: String = request.intermediaryNumber

  private val aMatch: Match = arbitraryMatch.arbitrary.sample.value.copy(intermediary = Some(intermediaryNumber))

  private val index: Index = Index(0)

  private val previousEuCountry1: Country = arbitraryCountry.arbitrary.sample.value
  private val previousEuCountry2: Country = arbitraryCountry.arbitrary.retryUntil(_.code != previousEuCountry1.code).sample.value
  private val previousSchemeDetails1: PreviousSchemeDetails = arbitraryPreviousSchemeDetails.arbitrary.sample.value
    .copy(
      previousScheme = IOSSWI
    )

  private val previousSchemeDetails2: PreviousSchemeDetails = previousSchemeDetails1
    .copy(
      previousScheme = OSSU,
      previousSchemeNumbers = arbitraryPreviousSchemeNumbers.arbitrary.sample.value
    )

  private val previousSchemeDetails3: PreviousSchemeDetails = previousSchemeDetails2
    .copy(
      previousScheme = OSSNU,
      previousSchemeNumbers = arbitraryPreviousSchemeNumbers.arbitrary.sample.value
    )

  private val previousRegistration1: PreviousRegistration = PreviousRegistration(
    previousEuCountry = previousEuCountry1,
    previousSchemesDetails = Seq(previousSchemeDetails1, previousSchemeDetails2, previousSchemeDetails3)
  )

  private val previousRegistration2: PreviousRegistration = previousRegistration1
    .copy(
      previousEuCountry = previousEuCountry2
    )

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "CoreSavedAnswersRevalidationService" - {

    ".checkAndValidateSavedUserAnswers" - {

      "must return None if there are no active matches, quarantines or expired VRNs" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

        result `mustBe` None
      }

      "when checking ClientVatNumberPage" - {

        "must revalidate UK VRN when an expired deregistration date exists" in {

          val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
          val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
            deregistrationDecisionDate = Some(today)
          )

          val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .copy(vatInfo = Some(vatCustomerInfoWithDeregistration))
            .set(ClientVatNumberPage, activeVrn.vrn).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` Some(routes.ExpiredVrnDateController.onPageLoad(waypoints).url)
          verifyNoInteractions(mockCoreRegistrationValidationService)
        }

        "must revalidate UK VRN if one exists and an active match is found" in {

          val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientVatNumberPage, activeVrn.vrn).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM${activeVrn.vrn}"),
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
          verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(activeVrn))(any(), eqTo(dataRequest))
        }

        "must revalidate UK VRN if one exists and no active match is found" in {

          val nonActiveVrn: Vrn = arbitraryVrn.arbitrary.sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientVatNumberPage, nonActiveVrn.vrn).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(nonActiveVrn))(any(), eqTo(dataRequest))
        }
      }

      "when checking ClientUtrNumberPage" - {

        "must revalidate Client UTR if one exists and an active match is found" in {

          val activeUtr: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientUtrNumberPage, activeUtr).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM$activeUtr"),
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
          verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(activeUtr))(any(), eqTo(dataRequest))
        }

        "must revalidate Client UTR if one exists and no active match is found" in {

          val nonActiveUtr: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientUtrNumberPage, nonActiveUtr).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(nonActiveUtr))(any(), eqTo(dataRequest))
        }
      }

      "when checking ClientsNinoNumberPage" - {

        "must revalidate Client NINO if one exists and an active match is found" in {

          val activeNino: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientsNinoNumberPage, activeNino).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM$activeNino"),
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
          verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(activeNino))(any(), eqTo(dataRequest))
        }

        "must revalidate Client NINO if one exists and no active match is found" in {

          val nonActiveNino: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientsNinoNumberPage, nonActiveNino).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(nonActiveNino))(any(), eqTo(dataRequest))
        }
      }

      "when checking ClientTaxReferencePage" - {

        "must revalidate Client Tax Reference if one exists and an active match is found" in {

          val country: Country = arbitraryCountry.arbitrary.sample.value
          val activeClientTaxReference: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientTaxReferencePage, activeClientTaxReference).success.value
            .set(ClientCountryBasedPage, country).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM$activeClientTaxReference"),
            memberState = country.code,
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchForeignTaxReference(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
          verify(mockCoreRegistrationValidationService, times(1)).searchForeignTaxReference(eqTo(activeClientTaxReference), eqTo(activeMatch.memberState))(any(), eqTo(dataRequest))
        }

        "must revalidate Client Tax Reference if one exists and no active match is found" in {

          val country: Country = arbitraryCountry.arbitrary.sample.value
          val nonActiveClientTaxReference: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientTaxReferencePage, nonActiveClientTaxReference).success.value
            .set(ClientCountryBasedPage, country).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          when(mockCoreRegistrationValidationService.searchForeignTaxReference(any(), any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchForeignTaxReference(eqTo(nonActiveClientTaxReference), eqTo(country.code))(any(), eqTo(dataRequest))
        }

        "must throw an IllegalStateException when the trader has a saved Client Tax Reference but the " +
          "corresponding country could not be retrieved from the user answers" in {

          val errorMessage: String = "Country could not be retrieved from user answers."

          val nonActiveClientTaxReference: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientTaxReferencePage, nonActiveClientTaxReference).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          intercept[IllegalStateException] {
            service.checkAndValidateSavedUserAnswers(waypoints)
          }.getMessage `mustBe` errorMessage

          verifyNoInteractions(mockCoreRegistrationValidationService)
        }
      }

      "when checking AllEuDetails" - {

        "must iterate through all existing EU Details and revalidate any EU VAT Numbers and EU Tax References present within the user answers" - {

          "and then return None when no active matches are found" in {

            val euVrn: String = arbitraryEuVatNumber.sample.value
            val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

            val euTaxReference: String = arbitraryEuTaxReference.sample.value
            val country2: Country = arbitraryCountry.arbitrary.sample.value

            val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress =
              arbitraryTradingNameAndBusinessAddress.arbitrary.sample.value

            val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(index), country1).success.value
              .set(TradingNameAndBusinessAddressPage(index), tradingNameAndBusinessAddress).success.value
              .set(RegistrationTypePage(index), VatNumber).success.value
              .set(EuVatNumberPage(index), euVrn).success.value
              .set(AddEuDetailsPage(), true).success.value
              .set(EuCountryPage(index + 1), country2).success.value
              .set(TradingNameAndBusinessAddressPage(index + 1), tradingNameAndBusinessAddress).success.value
              .set(RegistrationTypePage(index + 1), TaxId).success.value
              .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
              .set(AddEuDetailsPage(), false).success.value

            val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            implicit val dataRequest: DataRequest[AnyContent] =
              DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

            result `mustBe` None
            verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), any())
            verifyNoMoreInteractions(mockCoreRegistrationValidationService)
          }

          "and then return the corresponding URL when an active match is found" in {

            val euVrn: String = arbitraryEuVatNumber.sample.value
            val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

            val euTaxReference: String = arbitraryEuTaxReference.sample.value
            val country2: Country = arbitraryCountry.arbitrary.sample.value

            val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress =
              arbitraryTradingNameAndBusinessAddress.arbitrary.sample.value

            val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(index), country1).success.value
              .set(TradingNameAndBusinessAddressPage(index), tradingNameAndBusinessAddress).success.value
              .set(RegistrationTypePage(index), VatNumber).success.value
              .set(EuVatNumberPage(index), euVrn).success.value
              .set(AddEuDetailsPage(), true).success.value
              .set(EuCountryPage(index + 1), country2).success.value
              .set(TradingNameAndBusinessAddressPage(index + 1), tradingNameAndBusinessAddress).success.value
              .set(RegistrationTypePage(index + 1), TaxId).success.value
              .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
              .set(AddEuDetailsPage(), false).success.value

            val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            implicit val dataRequest: DataRequest[AnyContent] =
              DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            val activeMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = s"IM$euTaxReference"),
              memberState = country2.code,
              exclusionStatusCode = None,
              exclusionEffectiveDate = None
            )

            when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

            result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
            verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), any())
            verifyNoMoreInteractions(mockCoreRegistrationValidationService)
          }
        }
      }

      "when checking AllPreviousRegistrations" - {

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(PreviouslyRegisteredPage, true).success.value
          .set(PreviousEuCountryPage(index), previousEuCountry1).success.value
          .set(PreviousSchemePage(index, index), previousSchemeDetails1.previousScheme).success.value
          .set(PreviousSchemeTypePage(index, index), IOSS).success.value
          .set(ClientHasIntermediaryPage(index, index), true).success.value
          .set(PreviousIossNumberPage(index, index), previousSchemeDetails1.previousSchemeNumbers).success.value

          .set(PreviousSchemePage(index, index + 1), previousSchemeDetails2.previousScheme).success.value
          .set(PreviousSchemeTypePage(index, index + 1), OSS).success.value
          .set(PreviousOssNumberPage(index, index + 1), previousSchemeDetails2.previousSchemeNumbers).success.value

          .set(PreviousSchemePage(index, index + 2), previousSchemeDetails3.previousScheme).success.value
          .set(PreviousSchemeTypePage(index, index + 2), OSS).success.value
          .set(PreviousOssNumberPage(index, index + 2), previousSchemeDetails3.previousSchemeNumbers).success.value
          .set(AddPreviousRegistrationPage(), true).success.value

          .set(PreviousEuCountryPage(index + 1), previousEuCountry2).success.value
          .set(PreviousSchemePage(index + 1, index), previousSchemeDetails1.previousScheme).success.value
          .set(PreviousSchemeTypePage(index + 1, index), IOSS).success.value
          .set(ClientHasIntermediaryPage(index + 1, index), true).success.value
          .set(PreviousIossNumberPage(index + 1, index), previousSchemeDetails1.previousSchemeNumbers).success.value

          .set(PreviousSchemePage(index + 1, index + 1), previousSchemeDetails2.previousScheme).success.value
          .set(PreviousSchemeTypePage(index + 1, index + 1), OSS).success.value
          .set(PreviousOssNumberPage(index + 1, index + 1), previousSchemeDetails2.previousSchemeNumbers).success.value

          .set(PreviousSchemePage(index + 1, index + 2), previousSchemeDetails3.previousScheme).success.value
          .set(PreviousSchemeTypePage(index + 1, index + 2), OSS).success.value
          .set(PreviousOssNumberPage(index + 1, index + 2), previousSchemeDetails3.previousSchemeNumbers).success.value
          .set(AddPreviousRegistrationPage(), false).success.value

        "must iterate through all existing Previous Registrations and revalidate any previous OSS and IOSS numbers present" - {

          "and then return None when no active matches are found" in {

            val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            implicit val dataRequest: DataRequest[AnyContent] =
              DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

            result `mustBe` None
            verify(mockCoreRegistrationValidationService, times(6)).searchScheme(any(), any(), any(), any())(any(), any())
          }

          "when it is an OSS scheme" - {

            "and then None when an active match is found" in {

              val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              implicit val dataRequest: DataRequest[AnyContent] =
                DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              val activeMatch: Match = aMatch.copy(
                traderId = TraderId(traderId = previousRegistration2.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
                memberState = previousRegistration2.previousEuCountry.code,
                exclusionStatusCode = None,
                exclusionEffectiveDate = None
              )

              when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
              when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration2.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration2.previousSchemesDetails.tail.head.previousScheme),
                any(),
                eqTo(previousRegistration1.previousEuCountry.code)
              )(any(), any())) thenReturn Some(activeMatch).toFuture

              val service: CoreSavedAnswersRevalidationService =
                new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

              val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

              result `mustBe` None
              verify(mockCoreRegistrationValidationService, times(6)).searchScheme(any(), any(), any(), any())(any(), any())
            }

            "and then return the corresponding URL when a quarantined match is found" in {

              val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              implicit val dataRequest: DataRequest[AnyContent] = {
                DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)
              }

              val quarantinedMatch: Match = aMatch.copy(
                traderId = TraderId(traderId = previousRegistration1.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
                memberState = previousRegistration1.previousEuCountry.code,
                exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
                exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
              )

              when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
              when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration1.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration1.previousSchemesDetails.tail.head.previousScheme),
                any(),
                eqTo(previousRegistration1.previousEuCountry.code)
              )(any(), any())) thenReturn Some(quarantinedMatch).toFuture

              val service: CoreSavedAnswersRevalidationService =
                new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

              val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

              result `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                countryCode = quarantinedMatch.memberState,
                exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
              ).url)
              verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any(), any(), any())(any(), any())
            }
          }

          "when it is an IOSS scheme" - {

            "and then return the corresponding URL when an active match is found" in {

              val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              implicit val dataRequest: DataRequest[AnyContent] =
                DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              val activeMatch: Match = aMatch.copy(
                traderId = TraderId(traderId = s"IM${previousRegistration2.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber}"),
                memberState = previousRegistration2.previousEuCountry.code,
                exclusionStatusCode = None,
                exclusionEffectiveDate = None
              )

              when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
              when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration2.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration2.previousSchemesDetails.head.previousScheme),
                eqTo(Some(intermediaryNumber)),
                eqTo(previousRegistration2.previousEuCountry.code)
              )(any(), any())) thenReturn Some(activeMatch).toFuture

              val service: CoreSavedAnswersRevalidationService =
                new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

              val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

              result `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
              verify(mockCoreRegistrationValidationService, times(4)).searchScheme(any(), any(), any(), any())(any(), any())
            }

            "and then return the corresponding URL when a quarantined match is found" in {

              val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              implicit val dataRequest: DataRequest[AnyContent] =
                DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

              val quarantinedMatch: Match = aMatch.copy(
                traderId = TraderId(traderId = s"IM${previousRegistration1.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber}"),
                memberState = previousRegistration1.previousEuCountry.code,
                exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
                exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
              )

              when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration1.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration1.previousSchemesDetails.head.previousScheme),
                eqTo(Some(intermediaryNumber)),
                eqTo(previousRegistration1.previousEuCountry.code)
              )(any(), any())) thenReturn Some(quarantinedMatch).toFuture

              val service: CoreSavedAnswersRevalidationService =
                new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

              val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

              result `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                countryCode = quarantinedMatch.memberState,
                exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
              ).url)
              verify(mockCoreRegistrationValidationService, times(1)).searchScheme(any(), any(), any(), any())(any(), any())
            }
          }
        }
      }
    }

    ".revalidateUKVrn" - {

      "must return None if no active match is found" in {

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(waypoints, vrn, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), any())
      }

      "must return the URL for Expired Vrn Date page when the deregistration date is present and is on or before today" in {

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(waypoints, vrn, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ExpiredVrnDateController.onPageLoad().url)
        verifyNoInteractions(mockCoreRegistrationValidationService)
      }

      "must return None and then check for an active match when the deregistration date is present and is after today" in {

        val tomorrow: LocalDate = LocalDate.now(stubClockAtArbitraryDate).plusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(tomorrow)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(waypoints, vrn, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), any())
      }

      "must return the corresponding URL when an  active match is found" in {

        val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM${activeVrn.vrn}"),
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(waypoints, activeVrn, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(activeVrn))(any(), any())
      }
    }

    ".revalidateTraderId" - {

      "must return None if no active match is found" in {

        val ukReferenceNumber: String = arbitraryTraderId.arbitrary.sample.value.traderId

        when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateTraderId"))

        val result = service invokePrivate privateMethodCall(ukReferenceNumber, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(ukReferenceNumber))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val ukReferenceNumber: String = arbitraryTraderId.arbitrary.sample.value.traderId
        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$ukReferenceNumber"),
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchTraderId(any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateTraderId"))

        val result = service invokePrivate privateMethodCall(ukReferenceNumber, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          countryCode = quarantinedMatch.memberState,
          exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url)
        verify(mockCoreRegistrationValidationService, times(1)).searchTraderId(eqTo(ukReferenceNumber))(any(), any())
      }
    }

    ".revalidateForeignTaxReference" - {

      "must return None if no active match is found" in {

        val foreignTaxReference: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        when(mockCoreRegistrationValidationService.searchForeignTaxReference(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateForeignTaxReference"))

        val result = service invokePrivate privateMethodCall(foreignTaxReference, countryCode, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchForeignTaxReference(eqTo(foreignTaxReference), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val foreignTaxReference: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$foreignTaxReference"),
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchForeignTaxReference(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateForeignTaxReference"))

        val result = service invokePrivate privateMethodCall(foreignTaxReference, countryCode, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
        verify(mockCoreRegistrationValidationService, times(1)).searchForeignTaxReference(eqTo(foreignTaxReference), eqTo(countryCode))(any(), any())
      }
    }

    ".revalidateEuTaxId" - {

      "must return None if no active match is found" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuTaxId"))

        val result = service invokePrivate privateMethodCall(euTaxReference, countryCode, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euTaxReference"),
          memberState = countryCode,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuTaxId"))

        val result = service invokePrivate privateMethodCall(euTaxReference, countryCode, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          countryCode = quarantinedMatch.memberState,
          exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(countryCode))(any(), any())
      }
    }

    ".revalidateEuVrn" - {

      "must return None if no active match is found" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val countryCode: String = euVrn.substring(0, 2)

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuVrn"))

        val result = service invokePrivate privateMethodCall(euVrn, countryCode, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val countryCode: String = euVrn.substring(0, 2)

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euVrn"),
          memberState = countryCode,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuVrn"))

        val result = service invokePrivate privateMethodCall(euVrn, countryCode, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(countryCode))(any(), any())
      }
    }

    ".checkAllEuDetails" - {

      "must return None if there are no active matches found for all countries present in user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress =
          arbitraryTradingNameAndBusinessAddress.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(TradingNameAndBusinessAddressPage(index), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(TradingNameAndBusinessAddressPage(index + 1), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), eqTo(dataRequest))
      }

      "must return the corresponding URL when the first country with an active match is found in the user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress =
          arbitraryTradingNameAndBusinessAddress.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(TradingNameAndBusinessAddressPage(index), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(TradingNameAndBusinessAddressPage(index + 1), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euTaxReference"),
          memberState = country2.code,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), eqTo(dataRequest))
      }

      "must return the corresponding URL when the first country with a quarantined match is found in the user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val tradingNameAndBusinessAddress: TradingNameAndBusinessAddress =
          arbitraryTradingNameAndBusinessAddress.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(TradingNameAndBusinessAddressPage(index), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(TradingNameAndBusinessAddressPage(index + 1), tradingNameAndBusinessAddress).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryNumber, None)

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euVrn"),
          memberState = country1.code,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          countryCode = quarantinedMatch.memberState,
          exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }
    }

    ".revalidateEuDetails" - {

      "must return None when no active matches are found" in {

        val euVatNumber: String = arbitraryEuVatNumber.sample.value
        val euCountry: Country = Country.euCountries.find(_.code == euVatNumber.substring(0, 2)).head

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = Some(euVatNumber),
          euCountry = euCountry
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, Some(euVatNumber), hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when an active match is found for an EU VAT number" in {

        val euVatNumber: String = arbitraryEuVatNumber.sample.value
        val euCountry: Country = Country.euCountries.find(_.code == euVatNumber.substring(0, 2)).head

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = Some(euVatNumber),
          euCountry = euCountry
        )

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euVatNumber"),
          memberState = euCountry.code,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, Some(euVatNumber), hc, dataRequest)

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when an active match is found for an EU Tax Reference number" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val euCountry: Country = arbitraryCountry.arbitrary.sample.value

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = None,
          euTaxReference = Some(euTaxReference),
          euCountry = euCountry
        )

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM$euTaxReference"),
          memberState = euCountry.code,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, None, hc, dataRequest)

        result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          countryCode = quarantinedMatch.memberState,
          exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url)
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must throw an IllegalStateException when Eu Details exist with neither a Eu Vat Number or EU Tax Reference present" in {

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = None,
          euTaxReference = None
        )

        val errorMessage: String = s"$euDetails has neither a euVatNumber or euTaxReference."

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        intercept[IllegalStateException] {
          service invokePrivate privateMethodCall(euDetails, None, hc, dataRequest)
        }.getMessage `mustBe` errorMessage

        verifyNoInteractions(mockCoreRegistrationValidationService)
      }
    }

    ".checkAllPreviousRegistrations" - {

      val allPreviousRegistrations: Seq[PreviousRegistration] = Seq(previousRegistration1, previousRegistration2)

      "must iterate through all existing previous registrations and any encompassing previous scheme details" - {

        "and return None when no active matches are found" in {

          val previousEuCountry: Country = arbitraryCountry.arbitrary.sample.value
          val previousSchemeDetails: PreviousSchemeDetails = arbitraryPreviousSchemeDetails.arbitrary.sample.value

          val previousRegistration: PreviousRegistration = PreviousRegistration(
            previousEuCountry = previousEuCountry,
            previousSchemesDetails = Seq(previousSchemeDetails)
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllPreviousRegistrations"))

          val result = service invokePrivate privateMethodCall(Seq(previousRegistration), Some(intermediaryNumber), hc, dataRequest)

          result.futureValue `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
            eqTo(previousSchemeDetails.previousSchemeNumbers.previousSchemeNumber),
            eqTo(previousSchemeDetails.previousScheme),
            eqTo(Some(intermediaryNumber)),
            eqTo(previousEuCountry.code)
          )(any(), any())
        }

        "when it is an OSS scheme" - {

          "and return None when an active match is found" in {

            val activeMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = previousRegistration2.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
              memberState = previousRegistration2.previousEuCountry.code,
              exclusionStatusCode = None,
              exclusionEffectiveDate = None
            )

            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration2.previousSchemesDetails.tail.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration2.previousSchemesDetails.tail.head.previousScheme),
                any(),
                eqTo(previousRegistration2.previousEuCountry.code))
              (any(), any())) thenReturn Some(activeMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllPreviousRegistrations"))

            val result = service invokePrivate privateMethodCall(allPreviousRegistrations, None, hc, dataRequest)

            result.futureValue `mustBe` None
            verify(mockCoreRegistrationValidationService, times(6)).searchScheme(any(), any(), any(), any())(any(), any())
          }

          "and return the corresponding URL when a quarantined match is found" in {

            val quarantinedMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = previousRegistration2.previousSchemesDetails.tail.tail.head.previousSchemeNumbers.previousSchemeNumber),
              memberState = previousRegistration2.previousEuCountry.code,
              exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
              exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
            )

            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(
                eqTo(previousRegistration2.previousSchemesDetails.tail.tail.head.previousSchemeNumbers.previousSchemeNumber),
                eqTo(previousRegistration2.previousSchemesDetails.tail.tail.head.previousScheme),
                any(),
                eqTo(previousRegistration2.previousEuCountry.code))
              (any(), any())) thenReturn Some(quarantinedMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllPreviousRegistrations"))

            val result = service invokePrivate privateMethodCall(allPreviousRegistrations, None, hc, dataRequest)

            result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
              countryCode = quarantinedMatch.memberState,
              exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
            ).url)
            verify(mockCoreRegistrationValidationService, times(6)).searchScheme(any(), any(), any(), any())(any(), any())
          }
        }

        "when it is an IOSS scheme" - {

          "and return the corresponding URL when an active match is found" in {

            val activeMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = s"IM${previousSchemeDetails1.previousSchemeNumbers.previousSchemeNumber}"),
              memberState = previousRegistration1.previousEuCountry.code,
              exclusionStatusCode = None,
              exclusionEffectiveDate = None
            )

            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllPreviousRegistrations"))

            val result = service invokePrivate privateMethodCall(allPreviousRegistrations, Some(intermediaryNumber), hc, dataRequest)

            result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
            verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
              eqTo(previousRegistration1.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber),
              eqTo(previousRegistration1.previousSchemesDetails.head.previousScheme),
              eqTo(Some(intermediaryNumber)),
              eqTo(previousRegistration1.previousEuCountry.code)
            )(any(), any())
          }

          "and return the corresponding URL when a quarantined match is found" in {

            val quarantinedMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = s"IM${previousRegistration2.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber}"),
              memberState = previousRegistration2.previousEuCountry.code,
              exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
              exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
            )

            when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(
              eqTo(previousRegistration2.previousSchemesDetails.head.previousSchemeNumbers.previousSchemeNumber),
              eqTo(previousRegistration2.previousSchemesDetails.head.previousScheme),
              eqTo(Some(intermediaryNumber)),
              eqTo(previousRegistration2.previousEuCountry.code)
            )(any(), any())) thenReturn Some(quarantinedMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllPreviousRegistrations"))

            val result = service invokePrivate privateMethodCall(Seq(previousRegistration1, previousRegistration2), Some(intermediaryNumber), hc, dataRequest)

            result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
              countryCode = quarantinedMatch.memberState,
              exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
            ).url)
            verify(mockCoreRegistrationValidationService, times(4)).searchScheme(any(), any(), any(), any())(any(), any())
          }
        }
      }
    }

    ".revalidatePreviousSchemeDetails" - {

      val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

      val allPreviousSchemeDetails: Seq[PreviousSchemeDetails] = Seq(previousSchemeDetails1, previousSchemeDetails2, previousSchemeDetails3)

      "must return None when no active matches are found" in {

        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
        val previousSchemeDetails: PreviousSchemeDetails = arbitraryPreviousSchemeDetails.arbitrary.sample.value

        when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousSchemeDetails"))

        val result = service invokePrivate privateMethodCall(countryCode, Seq(previousSchemeDetails), None, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
          eqTo(previousSchemeDetails.previousSchemeNumbers.previousSchemeNumber),
          eqTo(previousSchemeDetails.previousScheme),
          any(),
          eqTo(countryCode)
        )(any(), any())
      }

      "when it is an OSS scheme" - {

        "must return None when an active match is found" in {

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = previousSchemeDetails3.previousSchemeNumbers.previousSchemeNumber),
            memberState = countryCode,
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousSchemeDetails"))

          val result = service invokePrivate privateMethodCall(countryCode, allPreviousSchemeDetails, None, hc, dataRequest)

          result.futureValue `mustBe` None
          verify(mockCoreRegistrationValidationService, times(3)).searchScheme(
            any(),
            any(),
            any(),
            eqTo(countryCode)
          )(any(), any())
        }

        "must return the corresponding URL when a quarantined match is found" in {

          val quarantinedMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = previousSchemeDetails2.previousSchemeNumbers.previousSchemeNumber),
            memberState = countryCode,
            exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
            exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchScheme(
            eqTo(previousSchemeDetails2.previousSchemeNumbers.previousSchemeNumber),
            eqTo(previousSchemeDetails2.previousScheme),
            any(),
            any()
          )(any(), any())) thenReturn Some(quarantinedMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousSchemeDetails"))

          val result = service invokePrivate privateMethodCall(countryCode, allPreviousSchemeDetails, None, hc, dataRequest)

          result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            countryCode = quarantinedMatch.memberState,
            exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
          ).url)
          verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any(), any(), eqTo(countryCode))(any(), any())
        }
      }

      "when it is an IOSS scheme" - {

        "must return the corresponding URL when an active match is found" in {

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM${previousSchemeDetails1.previousSchemeNumbers.previousSchemeNumber}"),
            memberState = countryCode,
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousSchemeDetails"))

          val result = service invokePrivate privateMethodCall(countryCode, allPreviousSchemeDetails, Some(intermediaryNumber), hc, dataRequest)

          result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
            eqTo(previousSchemeDetails1.previousSchemeNumbers.previousSchemeNumber),
            eqTo(previousSchemeDetails1.previousScheme),
            eqTo(Some(intermediaryNumber)),
            eqTo(countryCode)
          )(any(), any())
        }

        "must return the corresponding URL when a quarantined match is found" in {

          val quarantinedMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IM${previousSchemeDetails1.previousSchemeNumbers.previousSchemeNumber}"),
            memberState = countryCode,
            exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
            exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any(), any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousSchemeDetails"))

          val result = service invokePrivate privateMethodCall(countryCode, allPreviousSchemeDetails, Some(intermediaryNumber), hc, dataRequest)

          result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            countryCode = quarantinedMatch.memberState,
            exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
          ).url)
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
            eqTo(previousSchemeDetails1.previousSchemeNumbers.previousSchemeNumber),
            eqTo(previousSchemeDetails1.previousScheme),
            eqTo(Some(intermediaryNumber)),
            eqTo(countryCode)
          )(any(), any())
        }
      }
    }

    ".activeMatchRedirectUrl" - {

      "must return None when no active match is found" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(None)

        result.futureValue `mustBe` None
      }

      "must return the URL for Client Already Registered page when an active match exists and the trader is already registered" in {

        val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM${activeVrn.vrn}"),
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(Some(activeMatch))

        result.futureValue `mustBe` Some(routes.ClientAlreadyRegisteredController.onPageLoad().url)
      }

      "must return the URL for Other Country Excluded And Quarantined page when an active match exists and the trader is quarantined" in {

        val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IM${activeVrn.vrn}"),
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(Some(quarantinedMatch))

        result.futureValue `mustBe` Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
          countryCode = quarantinedMatch.memberState,
          exclusionEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url)
      }
    }

    ".checkVrnExpired" - {

      "must return false if there is no Vat Customer Info present" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(None)

        result `mustBe` false
      }

      "must return false if deregistration date is absent" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfo))

        result `mustBe` false
      }

      "must return false when deregistration date is present and is after today " in {

        val tomorrow: LocalDate = LocalDate.now(stubClockAtArbitraryDate).plusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(tomorrow)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` false
      }

      "must return true when deregistration date is present and is today " in {

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` true
      }

      "must return true when deregistration date is present and is before today " in {

        val yesterday: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(yesterday)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` true
      }
    }
  }
}
