package services.core

import controllers.routes
import jakarta.inject.Inject
import logging.Logging
import models.Country
import models.core.Match
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import pages.{ClientCountryBasedPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, Waypoints}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class CoreSavedAnswersRevalidationService @Inject()(
                                                     coreRegistrationValidationService: CoreRegistrationValidationService,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends Logging {

  // ClientVatNumberPage -> searchUkVrn
  // ClientUtrNumberPage -> searchTraderId
  // ClientsNinoNumberPage -> searchTraderId
  // ClientTaxReferencePage -> searchForeignTaxReference

  // USE THE MODELS FOR THESE -> LOOK AT IOSS-REG
  // EuTaxReferencePage -> searchEuTaxId
  // EuVatNumberPage -> searchEuVrn
  // PreviousIossNumberPage -> searchScheme
  // PreviousOssNumberPage -> searchScheme

  def checkAndValidateSavedUserAnswers(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    request.userAnswers.get(ClientVatNumberPage) match {
      case Some(value) =>
        revalidateUKVrn(waypoints, Vrn(value))
      case _ =>
        request.userAnswers.get(ClientUtrNumberPage) match {
          case Some(value) =>
            revalidateTraderId(value)
          case _ =>
            request.userAnswers.get(ClientsNinoNumberPage) match {
              case Some(value) =>
                revalidateTraderId(value)
              case _ =>
                request.userAnswers.get(ClientTaxReferencePage) match {
                  case Some(value) =>
                    val country: Country = request.userAnswers.get(ClientCountryBasedPage)
                      .getOrElse {
                        val message: String = "Country could not be retrieved from user answers."
                        logger.error(message)
                        val exception = new IllegalStateException(message)
                        throw exception
                      }
                    revalidateForeignTaxReference(value, country.code)

                  case _ =>
                    // TODO -> Call methods to iterate through List[EuDetails] and List[PrevReg]
                    None.toFuture // TODO -> Must return None when all avenues exhausted and user can continue with reg
                }
            }
        }
    }
  }

  private def revalidateUKVrn(waypoints: Waypoints, ukVrn: Vrn)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    if (checkVrnExpired(request.userAnswers.vatInfo)) {
      Some(routes.ExpiredVrnDateController.onPageLoad(waypoints).url).toFuture
    } else {
      coreRegistrationValidationService.searchUkVrn(ukVrn).flatMap { maybeActiveMatch =>
        activeMatchRedirectUrl(maybeActiveMatch)
      }
    }
  }

  private def revalidateTraderId(ukReferenceNumber: String)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    coreRegistrationValidationService.searchTraderId(ukReferenceNumber).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def revalidateForeignTaxReference(
                                             foreignReferenceNumber: String,
                                             countryCode: String
                                           )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    coreRegistrationValidationService.searchForeignTaxReference(foreignReferenceNumber, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def activeMatchRedirectUrl(maybeMatch: Option[Match]): Future[Option[String]] = {
    maybeMatch match {
      case Some(activeMatch) if activeMatch.isActiveTrader(clock) =>
        Some(routes.ClientAlreadyRegisteredController.onPageLoad().url).toFuture

      case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
        Some(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(activeMatch.memberState, activeMatch.getEffectiveDate).url).toFuture

      case _ => None.toFuture
    }
  }

  private def checkVrnExpired(vatCustomerInfo: Option[VatCustomerInfo]): Boolean = {
    vatCustomerInfo match {
      case Some(vatInfo) =>
        vatInfo.deregistrationDecisionDate.exists(!_.isAfter(LocalDate.now(clock)))
      case _ => false
    }
  }
}
