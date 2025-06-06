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
import models.domain.VatCustomerInfo
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus, VatCustomerNotFound}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object VatCustomerInfoHttpParser extends Logging {

  type VatCustomerInfoResponse = Either[ErrorResponse, VatCustomerInfo]

  implicit object VatCustomerInfoResponseReads extends HttpReads[VatCustomerInfoResponse] {

    override def read(method: String, url: String, response: HttpResponse): VatCustomerInfoResponse =
      response.status match {
        case OK =>
          response.json.validate[VatCustomerInfo] match {
            case JsSuccess(value, _) =>
              Right(value)
            case JsError(errors) =>
              logger.error(s"Could not read payload as a VatCustomerInfo model $errors", errors)
              Left(InvalidJson)
          }

        case NOT_FOUND =>
          logger.error(s"not found vat customer info")
          Left(VatCustomerNotFound)

        case status =>
          logger.error(s"There was error getting vat info status: $status with body: ${response.body}")
          Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
      }
  }
}
