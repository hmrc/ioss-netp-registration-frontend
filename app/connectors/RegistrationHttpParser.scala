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

package connectors

import logging.Logging
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.responses.*
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object RegistrationHttpParser extends Logging {
  
  type IossEtmpDisplayRegistrationResultResponse = Either[ErrorResponse, IossEtmpDisplayRegistration]
  type OssRegistrationResponse = Either[ErrorResponse, OssRegistration]

  implicit object IossEtmpDisplayRegistrationReads extends HttpReads[IossEtmpDisplayRegistrationResultResponse] {

    override def read(method: String, url: String, response: HttpResponse): IossEtmpDisplayRegistrationResultResponse =
      response.status match {
        case OK => response.json.validate[IossEtmpDisplayRegistration] match {
          case JsSuccess(etmpDisplayRegistration, _) => Right(etmpDisplayRegistration)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse IOSS Etmp Display Registration response JSON with body ${response.body}" +
              s" and status ${response.status} with errors: $errors")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"Unknown error occurred on IOSS Etmp Display Registration $status with body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected IOSS registration response, status $status returned"))
      }
  }

  implicit object OssRegistrationResponseReads extends HttpReads[OssRegistrationResponse] {

    override def read(method: String, url: String, response: HttpResponse): OssRegistrationResponse =
      response.status match {
        case OK => response.json.validate[OssRegistration] match {
          case JsSuccess(ossRegistration, _) => Right(ossRegistration)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse display registration response JSON with body ${response.body}" +
              s"and status ${response.status} with errors: $errors")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"Unknown error happened on display registration $status with body ${response.body}")
          Left(InternalServerError)
      }
  }
}

