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

package models.ossRegistration

import models.Address
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class OssVatDetails(
                       registrationDate: LocalDate,
                       address: Address,
                       partOfVatGroup: Boolean,
                       source: OssVatDetailSource
                     )

object OssVatDetails {
  implicit val format: OFormat[OssVatDetails] = Json.format[OssVatDetails]
}
