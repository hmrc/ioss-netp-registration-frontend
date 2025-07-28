/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.core.ValidateCoreRegistrationConnector
import logging.Logging
import models.CountryWithValidationDetails.convertTaxIdentifierForTransfer
import models.{Country, PreviousScheme}
import models.core.Match.ossDateFormatter
import models.core.{CoreRegistrationRequest, Match, MatchType, SourceType, TraderId}
import models.iossRegistration.IossEtmpExclusionReason
import models.requests.DataRequest
import services.ioss.IossRegistrationService
import services.oss.OssRegistrationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CoreRegistrationValidationService @Inject()(
                                                   connector: ValidateCoreRegistrationConnector,
                                                   iossRegistrationService: IossRegistrationService,
                                                   ossRegistrationService: OssRegistrationService,
                                                   clock: Clock
                                                 )(implicit ec: ExecutionContext) extends Logging {
  
  def searchUkVrn(vrn: Vrn)(implicit hc:HeaderCarrier, request: DataRequest[_]): Future[Option[Match]] = {
    
    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.VATNumber.toString, None, vrn.vrn, None, "GB")

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchTraderId(ukVatNumber: String)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Match]] = {

    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.TraderId.toString, None, ukVatNumber, None, "GB")

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchEuTaxId(euTaxReference: String, countryCode: String)(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Match]] = {

    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.EUTraderId.toString, None, euTaxReference, None, countryCode)

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchEuVrn(euVrn: String, countryCode: String)(implicit hc: HeaderCarrier,
                                                      request: DataRequest[_]): Future[Option[Match]] = {

    val convertedEuVrn = convertTaxIdentifierForTransfer(euVrn, countryCode)
    val coreRegistrationRequest = CoreRegistrationRequest(SourceType.EUTraderId.toString, None, convertedEuVrn, None, countryCode)

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  def searchScheme(searchNumber: String, previousScheme: PreviousScheme, intermediaryNumber: Option[String], countryCode: String)
                  (implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[Match]] = {

    if (previousScheme == PreviousScheme.OSSNU) {
      Future.successful(None)
    } else {

      if (countryCode == Country.northernIreland.code) {
        previousScheme match {
          case PreviousScheme.IOSSWOI | PreviousScheme.IOSSWI =>
            getIossRegistration(searchNumber)
          case PreviousScheme.OSSU | PreviousScheme.OSSNU =>
            getOssRegistration(searchNumber)
        }
      } else {

        val sourceType = previousScheme match {
          case PreviousScheme.OSSU | PreviousScheme.OSSNU => SourceType.EUTraderId
          case PreviousScheme.IOSSWOI | PreviousScheme.IOSSWI => SourceType.TraderId
        }

        val convertedSearchNumber = if (sourceType == SourceType.EUTraderId) {
          convertTaxIdentifierForTransfer(searchNumber, countryCode)
        } else {
          searchNumber
        }

        val coreRegistrationRequest = CoreRegistrationRequest(
          source = sourceType.toString,
          scheme = Some(convertScheme(previousScheme)),
          searchId = convertedSearchNumber,
          searchIntermediary = intermediaryNumber,
          searchIdIssuedBy = countryCode
        )

        getValidateCoreRegistrationResponse(coreRegistrationRequest)
      }
    }
  }

  private def convertScheme(previousScheme: PreviousScheme): String = {
    previousScheme match {
      case PreviousScheme.OSSU => "OSS"
      case PreviousScheme.OSSNU => "OSS"
      case PreviousScheme.IOSSWOI => "IOSS"
      case PreviousScheme.IOSSWI => "IOSS"
    }
  }

  private def getValidateCoreRegistrationResponse(coreRegistrationRequest: CoreRegistrationRequest)
                                                 (implicit hc: HeaderCarrier): Future[Option[Match]] = {
    connector.validateCoreRegistration(coreRegistrationRequest).map {
      case Right(coreRegistrationResponse) =>
        coreRegistrationResponse.matches.headOption
      case Left(errorResponse) =>
        logger.error(s"failed getting registration response $errorResponse")
        throw CoreRegistrationValidationException("Error while validating core registration")
    }
  }

  private def getIossRegistration(iossNumber: String)(implicit hc: HeaderCarrier): Future[Option[Match]] = {
    iossRegistrationService.getIossRegistration(iossNumber).map { registration =>

      val maybeExclusion = registration.exclusions.headOption
      val exclusionEffectiveDate = maybeExclusion.map(exclusion => exclusion.effectiveDate)
      val quarantineCutOffDate = LocalDate.now(clock).minusYears(2)
      val matchType = maybeExclusion match {
        case Some(exclusion) if exclusion.quarantine && exclusionEffectiveDate.exists(_.isAfter(quarantineCutOffDate)) =>
          MatchType.TraderIdQuarantinedNETP
        case _ =>
          MatchType.TraderIdActiveNETP
      }

      Some(
        Match(
          matchType = matchType,
          traderId = TraderId(iossNumber),
          intermediary = None,
          memberState = Country.northernIreland.code,
          exclusionStatusCode = maybeExclusion.map(exclusion => getExclusionStatusCode(exclusion.exclusionReason)),
          exclusionDecisionDate = maybeExclusion.map(exclusion => exclusion.decisionDate.format(ossDateFormatter)),
          exclusionEffectiveDate = maybeExclusion.map(exclusion => exclusion.effectiveDate.format(ossDateFormatter)),
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )
      )
    }
  }

  private def getOssRegistration(vrn: String)(implicit hc: HeaderCarrier): Future[Option[Match]] = {
    val normalizeVrn = vrn.stripPrefix(Country.northernIreland.code)
    ossRegistrationService.getLatestOssRegistration(Vrn(normalizeVrn)).map { registration =>

      val maybeExclusion = registration.excludedTrader
      val exclusionEffectiveDate = maybeExclusion.flatMap(_.effectiveDate)
      val quarantineCutOffDate = LocalDate.now(clock).minusYears(2)
      val isQuarantined = maybeExclusion.flatMap(_.quarantined).contains(true) && exclusionEffectiveDate.exists(_.isAfter(quarantineCutOffDate))
      val matchType = if (isQuarantined) {
        MatchType.TraderIdQuarantinedNETP
      } else {
        MatchType.TraderIdActiveNETP
      }

      Some(
        Match(
          matchType = matchType,
          traderId = TraderId(registration.vrn.vrn),
          intermediary = None,
          memberState = Country.northernIreland.code,
          exclusionStatusCode = maybeExclusion.flatMap(_.exclusionReason.map(_.numberValue)),
          exclusionDecisionDate = None,
          exclusionEffectiveDate = maybeExclusion.flatMap(_.effectiveDate.map(_.format(ossDateFormatter))),
          nonCompliantReturns = registration.nonCompliantReturns.flatMap(s => Try(s.toInt).toOption),
          nonCompliantPayments = registration.nonCompliantPayments.flatMap(s => Try(s.toInt).toOption)
        )
      )
    }
  }

  private def getExclusionStatusCode(reason: IossEtmpExclusionReason): Int = {
    reason.toString.toInt
  }
}

case class CoreRegistrationValidationException(message: String) extends Exception(message)