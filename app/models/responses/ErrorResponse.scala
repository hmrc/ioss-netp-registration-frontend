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

package models.responses

sealed trait ErrorResponse {
  def body: String
}

case object InvalidJson extends ErrorResponse {
  override val body: String = "Invalid JSON received"
}

case object NotFound extends ErrorResponse {
  override val body: String = "Not found"
}

case object ConflictFound extends ErrorResponse {
  override val body: String = "Conflict"
}

case object InternalServerError extends ErrorResponse {
  override val body: String = "Internal server error"
}

case object RegistrationNotFound extends ErrorResponse {
  override val body: String = "Registration not found"
}

case object ReceivedErrorFromCore extends ErrorResponse {
  override val body: String = "Received an error when submitting to core"
}

case object VatCustomerNotFound extends ErrorResponse {
  override val body: String = "Not found"
}

case class UnexpectedResponseStatus(status: Int, body: String) extends ErrorResponse

case class EisError(eisErrorResponse: EisErrorResponse) extends ErrorResponse {
  override val body: String =
    s"${eisErrorResponse.timestamp} " +
      s"${eisErrorResponse.error} " +
      s"${eisErrorResponse.errorMessage} "
}
