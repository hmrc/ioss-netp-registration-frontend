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

package services.core

import controllers.{SetActiveTraderResult, routes}
import jakarta.inject.Inject
import logging.Logging
import models.Country
import models.core.Match
import models.domain.VatCustomerInfo
import models.previousRegistrations.*
import models.requests.DataRequest
import models.vatEuDetails.EuDetails
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{ClientCountryBasedPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, Waypoints}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsWithOptionalVatNumberQuery
import repositories.SessionRepository
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class CoreSavedAnswersRevalidationService @Inject()(
                                                     coreRegistrationValidationService: CoreRegistrationValidationService,
                                                     sessionRepository: SessionRepository,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends SetActiveTraderResult with Logging {

  def checkAndValidateSavedUserAnswers(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    checkClientDetails(waypoints).flatMap {
      case None =>
        checkEuDetails().flatMap {
          case None =>
            checkPreviousRegistrations()

          case redirectUrl => redirectUrl.toFuture
        }

      case redirectUrl => redirectUrl.toFuture
    }
  }

  private def checkClientDetails(waypoints: Waypoints)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
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
                    val message: String = "There was an error when validating user answers." +
                      "Could not find a UK VRN, a UTR, a Nino or a UK Tax Reference in the user answers."
                    logger.error(message)
                    val exception: IllegalStateException = new IllegalStateException(message)
                    throw exception
                }
            }
        }
    }
  }

  private def checkEuDetails()(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    request.userAnswers.get(HasFixedEstablishmentPage) match {
      case Some(true) =>
        val euDetails: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)
        checkAllEuDetails(euDetails)

      case _ =>
        None.toFuture
    }
  }

  private def checkPreviousRegistrations()(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    request.userAnswers.get(PreviouslyRegisteredPage) match {
      case Some(true) =>
        val previousRegistrations =
          request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).getOrElse(List.empty)
        checkAllPreviousRegistrations(previousRegistrations, Some(request.intermediaryNumber))

      case _ =>
        None.toFuture
    }
  }

  private def revalidateUKVrn(waypoints: Waypoints, ukVrn: Vrn)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    if (checkVrnExpired(request.userAnswers.vatInfo)) {
      Some(Redirect(routes.ExpiredVrnDateController.onPageLoad(waypoints).url)).toFuture
    } else {
      coreRegistrationValidationService.searchUkVrn(ukVrn).flatMap { maybeActiveMatch =>
        activeMatchRedirectUrl(maybeActiveMatch)
      }
    }
  }

  private def revalidateTraderId(ukReferenceNumber: String)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchTraderId(ukReferenceNumber).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def revalidateForeignTaxReference(
                                             foreignReferenceNumber: String,
                                             countryCode: String
                                           )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchForeignTaxReference(foreignReferenceNumber, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def checkAllEuDetails(allEuDetails: List[EuDetails])(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
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
                                 )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
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

  private def checkAllPreviousRegistrations(
                                             allPreviousRegistrations: List[PreviousRegistrationDetailsWithOptionalVatNumber],
                                             intermediaryNumber: Option[String]
                                           )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    allPreviousRegistrations match {
      case ::(PreviousRegistrationDetailsWithOptionalVatNumber(
        country,
        Some(optionalSchemeDetails)
      ), remaining) =>
        revalidatePreviousSchemeDetails(
          countryCode = country.code,
          allPreviousSchemeDetails = optionalSchemeDetails,
          intermediaryNumber = intermediaryNumber
        ).flatMap {
          case Some(urlString) =>
            Some(urlString).toFuture

          case _ =>
            checkAllPreviousRegistrations(remaining, intermediaryNumber)
        }

      case ::(_, remaining) =>
        checkAllPreviousRegistrations(remaining, intermediaryNumber)

      case Nil => None.toFuture
    }
  }

  private def revalidatePreviousSchemeDetails(
                                               countryCode: String,
                                               allPreviousSchemeDetails: List[SchemeDetailsWithOptionalVatNumber],
                                               intermediaryNumber: Option[String]
                                             )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    allPreviousSchemeDetails match {
      case ::(SchemeDetailsWithOptionalVatNumber(Some(previousScheme), _, Some(SchemeNumbersWithOptionalVatNumber(Some(previousSchemeNumber)))), remaining) =>
        coreRegistrationValidationService.searchScheme(
          searchNumber = previousSchemeNumber,
          previousScheme = previousScheme,
          intermediaryNumber = intermediaryNumber,
          countryCode = countryCode
        ).flatMap { maybeActiveMatch =>
          activeMatchRedirectUrl(maybeActiveMatch).flatMap {
            case Some(urlString) =>
              Some(urlString).toFuture

            case _ =>
              revalidatePreviousSchemeDetails(countryCode, remaining, intermediaryNumber)
          }
        }

      case ::(_, remaining) =>
        revalidatePreviousSchemeDetails(countryCode, remaining, intermediaryNumber)

      case Nil => None.toFuture
    }
  }

  private def revalidateEuTaxId(
                                 euTaxReference: String,
                                 countryCode: String
                               )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuTaxId(euTaxReference, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def revalidateEuVrn(
                               euVrn: String,
                               countryCode: String
                             )(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def activeMatchRedirectUrl(maybeMatch: Option[Match])(implicit request: DataRequest[_]): Future[Option[Result]] = {
    maybeMatch match {
      case Some(activeMatch) if activeMatch.isActiveTrader(clock) =>
        setActiveTraderResultAndRedirect(
          activeMatch = activeMatch,
          sessionRepository = sessionRepository,
          redirect = controllers.routes.ClientAlreadyRegisteredController.onPageLoad()
        ).flatMap { result =>
          Some(result).toFuture
        }

      case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
        Some(Redirect(routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(activeMatch.memberState, activeMatch.getEffectiveDate).url)).toFuture

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
