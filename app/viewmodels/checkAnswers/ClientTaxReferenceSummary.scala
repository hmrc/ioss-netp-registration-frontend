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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.{CheckAnswersPage, ClientCountryBasedPage, ClientTaxReferencePage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ClientTaxReferenceSummary {

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           sourcePage: CheckAnswersPage,
         )(implicit messages: Messages): Option[SummaryListRow] = {

    for {
      taxRef <- answers.get(ClientTaxReferencePage)
      country <- answers.get(ClientCountryBasedPage)
    } yield {

      SummaryListRowViewModel(
        key = messages("clientTaxReference.checkYourAnswersLabel", country.name),
        value = ValueViewModel(HtmlFormat.escape(taxRef).toString),
        actions = Seq(
          ActionItemViewModel("site.change", ClientTaxReferencePage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("clientTaxReference.change.hidden"))
        )
      )
    }
  }
}
