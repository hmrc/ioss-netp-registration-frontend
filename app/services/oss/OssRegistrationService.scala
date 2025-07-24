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

package services.oss

import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.ossRegistration.OssRegistration
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class OssRegistrationService @Inject()(
                                           registrationConnector: RegistrationConnector,
                                           config: FrontendAppConfig
                                         )(implicit ec: ExecutionContext) extends Logging {

  def getLatestOssRegistration(vrn: Option[Vrn])(implicit hc: HeaderCarrier): Future[Option[OssRegistration]] = {
    vrn match {
      case Some(validVrn) =>
        registrationConnector.getOssRegistration(validVrn).map {
          case Right(registration) =>
            logger.info(s"Successfully fetched OSS registration for VRN ${validVrn.vrn}")
            Some(registration)
          case Left(error) =>
            logger.warn(s"Failed to fetch OSS registration for VRN ${validVrn.vrn}: $error")
            None
        }

      case None =>
        logger.warn("No VRN provided to fetch OSS registration")
        Future.successful(None)
    }
  }
}
