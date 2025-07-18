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
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case object BusinessBasedInUKPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessBasedInUK"

  override def route(waypoints: Waypoints): Call =
    routes.BusinessBasedInUKController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        ClientHasVatNumberPage
      case false =>
        ClientHasVatNumberPage
    }.orRecover

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(false) => for {
        removeClientVatNumberAnswers <- userAnswers.remove(ClientVatNumberPage)
        removeHasClientVatNumberAnswers <- removeClientVatNumberAnswers.remove(ClientHasVatNumberPage)
        removeCountryBasedInAnswers <- removeHasClientVatNumberAnswers.remove(ClientCountryBasedPage)
        removeHasUtrNumberAnswers <- removeCountryBasedInAnswers.remove(ClientHasUtrNumberPage)
        removeUtrNumberAnswers <- removeHasUtrNumberAnswers.remove(ClientUtrNumberPage)
        removeNinoNumberAnswers <- removeUtrNumberAnswers.remove(ClientsNinoNumberPage)
        removeBusinessName <- removeNinoNumberAnswers.remove(ClientBusinessNamePage)
        removeTaxReferenceAnswers <- removeBusinessName.remove(ClientTaxReferencePage)
        updatedUserAnswers <- removeTaxReferenceAnswers.remove(ClientBusinessAddressPage)
      } yield updatedUserAnswers
      case Some(true) => for {
        removeClientVatNumberAnswers <- userAnswers.remove(ClientVatNumberPage)
        removeHasClientVatNumberAnswers <- removeClientVatNumberAnswers.remove(ClientHasVatNumberPage)
        removeCountryBasedInAnswers <- removeHasClientVatNumberAnswers.remove(ClientCountryBasedPage)
        removeHasUtrNumberAnswers <- removeCountryBasedInAnswers.remove(ClientHasUtrNumberPage)
        removeUtrNumberAnswers <- removeHasUtrNumberAnswers.remove(ClientUtrNumberPage)
        removeNinoNumberAnswers <- removeUtrNumberAnswers.remove(ClientsNinoNumberPage)
        removeBusinessAddressAnswers <- removeNinoNumberAnswers.remove(ClientBusinessAddressPage)
        updatedUserAnswers <- removeBusinessAddressAnswers.remove(ClientBusinessNamePage)
      } yield updatedUserAnswers
      case _ => super.cleanup(value, userAnswers)
    }
  }
}
