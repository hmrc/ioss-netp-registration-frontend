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

package models.emails

import play.api.libs.json.*

case class ClientDeclarationEmailParameters(
                                   intermediary_name: String,
                                   recipientName_line1: String,
                                   activation_code: String,
                                   activation_code_expiry_date: String
                                 )

object ClientDeclarationEmailParameters {
  implicit val reads: Reads[ClientDeclarationEmailParameters] = Json.reads[ClientDeclarationEmailParameters]
  implicit val writes: Writes[ClientDeclarationEmailParameters] = Json.writes[ClientDeclarationEmailParameters]
}






