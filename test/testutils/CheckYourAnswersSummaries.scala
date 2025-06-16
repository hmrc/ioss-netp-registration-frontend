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

package testutils

import models.UserAnswers
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.govuk.SummaryListFluency

object CheckYourAnswersSummaries extends SummaryListFluency {

  def getCYAVatDetailsSummaryList(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)
                       (implicit msgs: Messages): Seq[SummaryListRow] = {

    val basedInUkSummaryRow = BusinessBasedInUKSummary.row(waypoints, answers, sourcePage)
    val hasVatNumberSummaryRow = ClientHasVatNumberSummary.row(waypoints, answers, sourcePage)
    val vatNumberSummaryRow = ClientVatNumberSummary.row(waypoints, answers, sourcePage)
    val businessAddressSumaryRow = VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, answers, sourcePage)
    val clientHasUtrNumberSummaryRow = ClientHasUtrNumberSummary.row(waypoints, answers, sourcePage)
    val clientUtrNumberSummaryRow = ClientUtrNumberSummary.row(waypoints, answers, sourcePage)
    val clientNinoNumberSummaryRow = ClientsNinoNumberSummary.row(waypoints, answers, sourcePage)
    val clientTaxReferenceSummaryRow = ClientTaxReferenceSummary.row(waypoints, answers, sourcePage)
    val clientBusinessNameSummaryRow = ClientBusinessNameSummary.row(waypoints, answers, sourcePage)
    val clientsBusinessAddressSummaryRow = ClientBusinessAddressSummary.row(waypoints, answers, sourcePage)
    
    Seq(
      basedInUkSummaryRow,
      hasVatNumberSummaryRow,
      vatNumberSummaryRow,
      businessAddressSumaryRow,
      clientHasUtrNumberSummaryRow,
      clientUtrNumberSummaryRow,
      clientNinoNumberSummaryRow,
      clientTaxReferenceSummaryRow,
      clientBusinessNameSummaryRow,
      clientsBusinessAddressSummaryRow
    ).flatten
  }
  
  def getCYASummaryList(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)
                       (implicit msgs: Messages): Seq[SummaryListRow] = {

    val websiteSummaryRow = WebsiteSummary.checkAnswersRow(waypoints, answers, sourcePage)
    val contactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowFullName(waypoints, answers, sourcePage)
    val contactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, sourcePage)
    val contactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, sourcePage)

    Seq(
      websiteSummaryRow,
      contactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsEmailSummaryRow
    ).flatten
  }
}
