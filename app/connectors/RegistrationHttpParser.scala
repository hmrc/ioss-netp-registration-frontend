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
import models.etmp.amend.AmendRegistrationResponse
import models.etmp.display.RegistrationWrapper
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.responses.*
import models.responses.etmp.EtmpEnrolmentResponse
import play.api.http.Status.*
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object RegistrationHttpParser extends Logging {

  type RegistrationResultResponse = Either[ErrorResponse, EtmpEnrolmentResponse]
  type IossEtmpDisplayRegistrationResultResponse = Either[ErrorResponse, IossEtmpDisplayRegistration]
  type OssRegistrationResponse = Either[ErrorResponse, OssRegistration]
  type AmendRegistrationResultResponse = Either[ErrorResponse, AmendRegistrationResponse]
  type EtmpDisplayRegistrationResponse = Either[ErrorResponse, RegistrationWrapper]

  implicit object RegistrationResponseReads extends HttpReads[RegistrationResultResponse] {

    override def read(method: String, url: String, response: HttpResponse): RegistrationResultResponse =
      response.status match {
        case CREATED => response.json.validate[EtmpEnrolmentResponse] match {
          case JsSuccess(enrolmentResponse, _) => Right(enrolmentResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse JSON, but was successfully created ${response.body} ${errors}", errors)
            Left(InvalidJson)
        }
        case CONFLICT =>
          logger.error(s"Received ConflictFound when trying to submit registration")
          Left(ConflictFound)
        case INTERNAL_SERVER_ERROR =>
          logger.error(s"Received InternalServerError when trying to submit registration with message: ${InternalServerError.body}")
          Left(InternalServerError)
        case status =>
          logger.error(s"Received unexpected error when trying to submit registration with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
  }

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

  implicit object AmendAmendRegistrationResultResponseReads extends HttpReads[AmendRegistrationResultResponse] {

    override def read(method: String, url: String, response: HttpResponse): AmendRegistrationResultResponse =
      response.status match {
        case OK => response.json.validate[AmendRegistrationResponse] match {
          case JsSuccess(amendResponse, _) => Right(amendResponse)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse amend registration response JSON with body ${response.body}" +
              s" and status ${response.status} with errors: $errors")
            Left(InvalidJson)
        }
        case status =>
          logger.error(s"Unknown error occurred on amend registration $status with body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected amend registration response, status $status returned"))
      }
  }

  implicit object EtmpDisplayRegistrationResponseReads extends HttpReads[EtmpDisplayRegistrationResponse] {

    override def read(method: String, url: String, response: HttpResponse): EtmpDisplayRegistrationResponse = {
      response.status match {
        case OK =>
          response.json.validate[RegistrationWrapper] match {
            case JsSuccess(registrationWrapper, _) => Right(registrationWrapper)
            case JsError(errors) =>
              logger.error(s"Failed trying to parse Registration Wrapper JSON with status: ${response.status} " +
                s"and response body: ${response.body} with errors: $errors.")
              Left(InvalidJson)
          }

        case status =>
          logger.error(s"An unknown error occurred when trying to retrieve Registration Wrapper with status: $status " +
            s"and response body: ${response.body}")
          Left(InternalServerError)
      }
    }
  }
}

