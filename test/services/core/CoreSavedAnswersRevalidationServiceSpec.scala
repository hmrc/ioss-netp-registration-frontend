package services.core

import base.SpecBase
import controllers.routes
import models.core.{Match, TraderId}
import models.domain.VatCustomerInfo
import models.ossRegistration.ExclusionReason
import models.requests.DataRequest
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{ClientCountryBasedPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreSavedAnswersRevalidationServiceSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, emptyUserAnswersWithVatInfo, intermediaryDetails.intermediaryNumber, None)

  implicit private val dataRequest: DataRequest[AnyContent] =
    DataRequest(request, vrn.vrn, emptyUserAnswersWithVatInfo, intermediaryDetails.intermediaryNumber, None)

  private val aMatch: Match = arbitraryMatch.arbitrary.sample.value.copy(intermediary = Some(intermediaryDetails.intermediaryNumber))

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "CoreSavedAnswersRevalidationService" - {

    // TODO
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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers(waypoints).futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(nonActiveVrn))(any(), eqTo(dataRequest))
        }
      }

      // TODO
      "when checking ClientUtrNumberPage" - {

        "must revalidate Client UTR if one exists and an active match is found" in {

          val activeUtr: String = arbitrary[String].sample.value
          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(ClientUtrNumberPage, activeUtr).success.value

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

          val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          implicit val dataRequest: DataRequest[AnyContent] =
            DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          intercept[IllegalStateException] {
            service.checkAndValidateSavedUserAnswers(waypoints)
          }.getMessage `mustBe` errorMessage

          verifyNoInteractions(mockCoreRegistrationValidationService)
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

      "must return the url for Expired Vrn Date page when the deregistration date is present and is on or before today" in {

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

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

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(waypoints, vrn, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), any())
      }

      "must return the corresponding url when an  active match is found" in {

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

      "must return the corresponding url when an active match is found" in {

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

      "must return the corresponding url when an active match is found" in {

        val foreignTaxReference: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = DataRequest(FakeRequest("GET", "/"), vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

        implicit val dataRequest: DataRequest[AnyContent] =
          DataRequest(request, vrn.vrn, updatedUserAnswers, intermediaryDetails.intermediaryNumber, None)

        when(mockCoreRegistrationValidationService.searchForeignTaxReference(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateForeignTaxReference"))

        val result = service invokePrivate privateMethodCall(foreignTaxReference, countryCode, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchForeignTaxReference(eqTo(foreignTaxReference), eqTo(countryCode))(any(), any())
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

      "must return the url for Client Already Registered page when an active match exists and the trader is already registered" in {

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

      "must return the url for Other Country Excluded And Quarantined page when an active match exists and the trader is quarantined" in {

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
