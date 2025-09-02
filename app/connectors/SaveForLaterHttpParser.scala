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
import models.SavedUserAnswers
import models.responses.*
import play.api.http.Status.{CONFLICT, CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SaveForLaterHttpParser extends Logging {

  type SaveForLaterResponse = Either[ErrorResponse, Option[SavedUserAnswers]]
  type DeleteSaveForLaterResponse = Either[ErrorResponse, Boolean]

  implicit object SaveForLaterHttpReads extends HttpReads[SaveForLaterResponse] {
    override def read(method: String, url: String, response: HttpResponse): SaveForLaterResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[SavedUserAnswers] match {
            case JsSuccess(answers, _) => Right(Some(answers))
            case JsError(errors) =>
              logger.error(s"Failed trying to parse JSON with error: $errors. JSON was ${response.json}.", errors)
              Left(InvalidJson)
          }

        case NOT_FOUND =>
          logger.warn(s"Received NotFound for saved user answers.")
          Right(None)

        case CONFLICT =>
          logger.warn(s"Received ConflictFound from server.")
          Left(ConflictFound)

        case status =>
          logger.error(s"Received unexpected error from saved user answers server with status: $status.")
          Left(UnexpectedResponseStatus(status, s"Unexpected response received with status: $status."))
      }
    }
  }

  implicit object DeleteSaveForLaterReads extends HttpReads[DeleteSaveForLaterResponse] {
    override def read(method: String, url: String, response: HttpResponse): DeleteSaveForLaterResponse = {
      response.status match {
        case OK =>
          response.json.validate[Boolean] match {
            case JsSuccess(deleted, _) => Right(deleted)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON with error: $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }

        case NOT_FOUND =>
          logger.warn("Received NotFound when deleting saved user answers")
          Left(NotFound)

        case CONFLICT =>
          logger.warn("Received ConflictFound from server when deleting saved user answers")
          Left(ConflictFound)

        case status =>
          logger.error(s"Received unexpected error when deleting saved user answers with status: $status.")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response received when deleting saved user answers with status: $status."))
      }
    }
  }
}
