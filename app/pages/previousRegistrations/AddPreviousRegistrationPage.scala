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
import models.{Country, Index, UserAnswers}
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.{AddItemPage, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousRegistrations.DeriveNumberOfPreviousRegistrations

object AddPreviousRegistrationPage {
  val normalModeUrlFragment: String = "previous-schemes-overview"
  val checkModeUrlFragment: String = "change-previous-schemes-overview"
}

case class AddPreviousRegistrationPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddPreviousRegistrationPage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = AddPreviousRegistrationPage.normalModeUrlFragment
  override val checkModeUrlFragment: String = AddPreviousRegistrationPage.checkModeUrlFragment
  
  override def path: JsPath = JsPath \ toString

  override def toString: String = "addPreviousRegistration"

  override def route(waypoints: Waypoints): Call =
    routes.AddPreviousRegistrationController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true =>
        index
          .map { i =>
            if (i.position + 1 < Country.euCountries.size) {
              PreviousEuCountryPage(Index(i.position + 1))
            } else {
              HasFixedEstablishmentPage
            }
          }
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => PreviousEuCountryPage(Index(n)))
              .orRecover
          }
      case false => HasFixedEstablishmentPage
    }.orRecover
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfPreviousRegistrations

}