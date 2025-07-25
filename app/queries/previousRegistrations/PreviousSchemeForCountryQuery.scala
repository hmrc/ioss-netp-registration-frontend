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

import models.domain.PreviousSchemeDetails
import models.{Index, UserAnswers}
import play.api.libs.json.JsPath
import queries.{Gettable, Settable}

import scala.util.Try

case class PreviousSchemeForCountryQuery(countryIndex: Index, schemeIndex: Index) extends Gettable[PreviousSchemeDetails] with Settable[PreviousSchemeDetails] {

  override def path: JsPath = JsPath \ "previousRegistrations" \ countryIndex.position \ "previousSchemesDetails" \ schemeIndex.position

  override def cleanup(value: Option[PreviousSchemeDetails], userAnswers: UserAnswers): Try[UserAnswers] = {
    userAnswers.get(DeriveNumberOfPreviousSchemes(countryIndex)) match {
      case Some(0) =>
        userAnswers.remove(PreviousRegistrationQuery(countryIndex))
      case _ => super.cleanup(value, userAnswers)

    }
  }
}

