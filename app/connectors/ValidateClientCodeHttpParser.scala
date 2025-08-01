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
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ValidateClientCodeHttpParser extends Logging {

  type validateClientCodeResponse = Either[ErrorResponse, Boolean]

  implicit object ValidateClientCodeReads extends HttpReads[validateClientCodeResponse] {

    override def read(method: String, url: String, response: HttpResponse): validateClientCodeResponse = {
      response.status match {
        case OK => response.json.validate[Boolean] match {
          case JsSuccess(boolean, _) => Right(boolean)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse validation as a boolean")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"Received unexpected response when validating registration code. Response body: ${response.body}, Error Status: $status")
          Left(UnexpectedResponseStatus(
            response.status,
            s"Unexpected response when trying to validate registration code, status $status returned")
          )
      }
    }
  }
}
