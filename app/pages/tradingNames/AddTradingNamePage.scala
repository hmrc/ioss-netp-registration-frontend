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

package pages.tradingNames

import controllers.tradingNames.routes
import models.{Index, UserAnswers}
import pages.previousRegistrations.PreviouslyRegisteredPage
import pages.{AddItemPage, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.tradingNames.DeriveNumberOfTradingNames

final case class AddTradingNamePage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddTradingNamePage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = "add-uk-trading-name"
  override val checkModeUrlFragment: String = "change-add-uk-trading-name"

  override def path: JsPath = JsPath \ toString

  override def toString: String = "addTradingName"

  override def route(waypoints: Waypoints): Call = {
    routes.AddTradingNameController.onPageLoad(waypoints)
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        index
          .map { i =>
            if (i.position + 1 < 10) {
              TradingNamePage(Index(i.position + 1))
            } else {
              PreviouslyRegisteredPage
            }
          }
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => TradingNamePage(Index(n)))
              .orRecover
          }

      case false =>
        PreviouslyRegisteredPage
    }.orRecover


  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfTradingNames
}