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
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SavedPendingRegistrationsHttpParser extends Logging {

  type SavedPendingRegistrationsResponse = Either[ErrorResponse, Seq[SavedPendingRegistration]]

  implicit object SavedPendingRegistrationResultResponseReads extends HttpReads[SavedPendingRegistrationsResponse] {

    def operation(action: String) = action.toUpperCase match {
      case "POST" => "create"
      case "GET" => "retrieve"
      case other => other.toLowerCase
    }

    override def read(method: String, url: String, response: HttpResponse): SavedPendingRegistrationsResponse = {
      response.status match {
        case OK | CREATED => response.json.validate[Seq[SavedPendingRegistration]] match {
          case JsSuccess(savedPendingRegistration, _) => Right(savedPendingRegistration)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse Pending Registrations JSON with errors $errors.")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"Received unexpected error when trying to ${operation(method)} Pending Registrations with the given intermediary number " +
            s"with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(
            response.status,
            s"Unexpected response when trying to ${operation(method)} pending registrations, status $status returned")
          )
      }
    }
  }
}
