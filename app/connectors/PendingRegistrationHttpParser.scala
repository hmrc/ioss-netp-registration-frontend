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
import models.SavedPendingRegistration
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object PendingRegistrationHttpParser extends Logging {

  type PendingRegistrationResultResponse = Either[ErrorResponse, Unit]
  type SavedPendingRegistrationResponse = Either[ErrorResponse, SavedPendingRegistration]

  implicit object PendingRegistrationResultResponseReads extends HttpReads[PendingRegistrationResultResponse] {

    override def read(method: String, url: String, response: HttpResponse): PendingRegistrationResultResponse = {
      response.status match {
        case NO_CONTENT => Right(())

        case status =>
          logger.error(s"Received unexpected error when trying to submit a Pending Registration " +
            s"with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response when submitting the pending registration, status $status returned"))
      }
    }
  }

  implicit object SavedPendingRegistrationResponseReads extends HttpReads[SavedPendingRegistrationResponse] {

    override def read(method: String, url: String, response: HttpResponse): SavedPendingRegistrationResponse = {
      response.status match {
        case OK => response.json.validate[SavedPendingRegistration] match {
          case JsSuccess(savedPendingRegistration, _) => Right(savedPendingRegistration)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse Saved Pending Registration JSON with errors $errors.")
            Left(InvalidJson)
        }
        
        case status =>
          logger.error(s"Received unexpected error when trying to retrieve a Saved Pending Registration with the given journey id " +
            s"with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response when retrieving the saved pending registration, status $status returned"))
      }
    }
  }
}
