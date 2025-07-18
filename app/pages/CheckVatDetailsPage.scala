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

package pages

import controllers.routes
import models.UserAnswers
import models.checkVatDetails.CheckVatDetails
import pages.tradingNames.HasTradingNamePage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class CheckVatDetailsPage() extends CheckAnswersPage with QuestionPage[CheckVatDetails] {

  override val urlFragment: String = "check-vat-details"

  override def isTheSamePage(other: Page): Boolean = other match {
    case CheckVatDetailsPage() => true
    case _ => false
  }

  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkVatDetails"

  override def route(waypoints: Waypoints): Call =
    routes.CheckVatDetailsController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    HasTradingNamePage

}