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

package pages.previousRegistrations

import controllers.previousRegistrations.routes
import models.{Index, PreviousSchemeType, UserAnswers}
import pages.{Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class PreviousSchemeTypePage(countryIndex: Index, schemeIndex: Index) extends QuestionPage[PreviousSchemeType] {
  
  override def path: JsPath = JsPath \ "previousRegistrations" \ countryIndex.position \ "previousSchemesDetails" \ schemeIndex.position \ toString

  override def toString: String = "previousSchemeType"

  override def route(waypoints: Waypoints): Call =
    routes.PreviousSchemeController.onPageLoad(waypoints, countryIndex, schemeIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    if(answers.get(this).contains(PreviousSchemeType.OSS)) {
      PreviousOssNumberPage(countryIndex, schemeIndex)
    } else {
      ClientHasIntermediaryPage(countryIndex, schemeIndex)
    }
}
