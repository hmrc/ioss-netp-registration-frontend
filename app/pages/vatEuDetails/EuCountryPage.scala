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

package pages.vatEuDetails

import controllers.vatEuDetails.routes
import models.{Country, Index, UserAnswers}
import pages.{Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class EuCountryPage(countryIndex: Index) extends QuestionPage[Country] {

  override def path: JsPath = JsPath \ "euDetails" \ countryIndex.position \ toString

  override def toString: String = "euCountry"

  override def route(waypoints: Waypoints): Call = {
    routes.EuCountryController.onPageLoad(waypoints, countryIndex)
  }
  
  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    TradingNameAndBusinessAddressPage(countryIndex)
  }
}
