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
import viewmodels.checkAnswers.BusinessContactDetailsSummary
import viewmodels.govuk.SummaryListFluency

object CheckYourAnswersSummaries extends SummaryListFluency {

  def getCYASummaryList(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)
                       (implicit msgs: Messages): Seq[SummaryListRow] = {

    val contactDetailsContactNameSummaryRow = BusinessContactDetailsSummary.rowFullName(waypoints, answers, sourcePage)
    val contactDetailsTelephoneSummaryRow = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, sourcePage)
    val contactDetailsEmailSummaryRow = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, sourcePage)

    Seq(
      contactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsEmailSummaryRow
    ).flatten
  }
}
