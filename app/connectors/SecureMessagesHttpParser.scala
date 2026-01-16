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

package connectors

import logging.Logging
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import models.securemessage.SecureMessageResponseWithCount
import play.api.http.Status.{CREATED, NO_CONTENT, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SecureMessagesHttpParser extends Logging {

  type SecureMessageResultResponse = Either[ErrorResponse, SecureMessageResponseWithCount]
  type MarkAsReadResponse = Either[ErrorResponse, Unit]
  type GetMessageResponse = Either[ErrorResponse, String]
  
  implicit object SecureMessageResultResponseReads extends HttpReads[SecureMessageResultResponse] {

    def operation(action: String) = action.toUpperCase match {
      case "POST" => "create"
      case "GET" => "retrieve"
      case other => other.toLowerCase
    }
    
    override def read(method: String, url: String, response: HttpResponse): SecureMessageResultResponse = {

      response.status match {
        case OK | CREATED => response.json.validate[SecureMessageResponseWithCount] match {
          case JsSuccess(secureMessage, _) => Right(secureMessage)
          case JsError(error) =>
            logger.error(s"Failed trying to parse Secure Message JSON with error: $error")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"Received unexpected error when trying to ${operation(method)} a Secure Message with status $status and body ${response.body}")
          Left(UnexpectedResponseStatus(
            response.status,
            s"Unexpected response when trying to ${operation(method)} secure messages, status $status returned")
          )
      }
    }
  }

  implicit object MarkAsReadResponseReads extends HttpReads[MarkAsReadResponse] {

    override def read(method: String, url: String, response: HttpResponse): MarkAsReadResponse = {

      response.status match {
        case OK | NO_CONTENT =>
          Right(())
        case status =>
          logger.error(s"Unexpected response marking message as read, status $status and body: ${response.body}")
          Left(UnexpectedResponseStatus(
            response.status,
            s"Unexpected response when marking secure message as read, status $status returned"
          )
          )
      }
    }
  }

  implicit object GetMessageResponseReads extends HttpReads[GetMessageResponse] {

    override def read(method: String, url: String, response: HttpResponse): GetMessageResponse = {

      response.status match {
        case OK =>
          Right(response.body)

        case status =>
          logger.error(s"Unexpected status $status retrieving secure message content from $url: ${response.body}")
          Left(UnexpectedResponseStatus(
            status,
            s"Unexpected response retrieving secure message content $url, status $status returned")
          )
      }
    }
  }
}
