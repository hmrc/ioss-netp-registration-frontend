package services.core

import controllers.routes
import jakarta.inject.Inject
import logging.Logging
import models.Country
import models.core.Match
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import models.vatEuDetails.EuDetails
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{ClientCountryBasedPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, Waypoints}
import queries.euDetails.AllEuDetailsQuery
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
                    request.userAnswers.get(HasFixedEstablishmentPage) match {
                      case Some(true) =>
                        val euDetails = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)
                        // TODO -> Should this be raw details as user may have only saved partial EuDetails?
                        checkAllEuDetails(euDetails)

                      case _ =>
                        // TODO -> Call method to iterate through List[PrevReg]
                        // TODO -> Must return None when all avenues exhausted and user can continue with reg
                        None.toFuture
                    }
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

  private def checkAllEuDetails(allEuDetails: List[EuDetails])(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    allEuDetails match {
      case ::(currentEuDetails, remaining) =>
        revalidateEuDetails(currentEuDetails, currentEuDetails.euVatNumber).flatMap {
          case Some(urlString) => Some(urlString).toFuture
          case _ => checkAllEuDetails(remaining)
        }

      case Nil => None.toFuture
    }
  }

  private def revalidateEuDetails(
                                   euDetails: EuDetails,
                                   euVatNumber: Option[String]
                                 )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[String]] = {
    euVatNumber match {
      case Some(euVrn) =>
        revalidateEuVrn(euVrn, euDetails.euCountry.code)

      case _ => euDetails.euTaxReference match {
        case Some(euTaxReference) =>
          revalidateEuTaxId(euTaxReference, euDetails.euCountry.code)

        case _ =>
          val errorMessage: String = s"$euDetails has neither a euVatNumber or euTaxReference."
          logger.error(errorMessage)
          val exception = IllegalStateException(errorMessage)
          throw exception
      }
    }
  }

  private def revalidateEuTaxId(euTaxReference: String, countryCode: String)(implicit hc: HeaderCarrier, request: DataRequest[_]) = {
    coreRegistrationValidationService.searchEuTaxId(euTaxReference, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def revalidateEuVrn(euVrn: String, countryCode: String)(implicit hc: HeaderCarrier, request: DataRequest[_]) = {
    coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).flatMap { maybeActiveMatch =>
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
