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

package queries.previousRegistrations

import models.Index
import models.previousRegistrations.SchemeDetailsWithOptionalVatNumber
import play.api.libs.json.JsPath
import queries.{Gettable, Settable}

case class AllPreviousSchemesForCountryWithOptionalVatNumberQuery(index: Index) extends Gettable[List[SchemeDetailsWithOptionalVatNumber]]
  with Settable[List[SchemeDetailsWithOptionalVatNumber]] {

  override def path: JsPath = JsPath \ "previousRegistrations" \ index.position \ "previousSchemesDetails"
}

