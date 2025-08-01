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

package viewmodels.checkAnswers.vatEuDetails

import models.{Index, UserAnswers}
import pages.vatEuDetails.{AddEuDetailsPage, CheckEuDetailsAnswersPage, DeleteEuDetailsPage}
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object EuDetailsSummary {

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           sourcePage: AddItemPage
         )(implicit messages: Messages): SummaryList = {
    SummaryList(
      answers.get(AllEuDetailsQuery).getOrElse(List.empty).zipWithIndex.map {
        case (euDetails, countryIndex) =>

          val value = euDetails.euVatNumber.getOrElse("") + euDetails.euTaxReference.getOrElse("")

          SummaryListRowViewModel(
            key = euDetails.euCountry.name,
            value = ValueViewModel(HtmlContent(value)),
            actions = Seq(
              ActionItemViewModel("site.change", CheckEuDetailsAnswersPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages("change.euDetails.hidden", euDetails.euCountry.name)),

              ActionItemViewModel("site.remove", DeleteEuDetailsPage(Index(countryIndex)).route(waypoints).url)
                .withVisuallyHiddenText(messages("remove.euDetails.hidden", euDetails.euCountry.name))
            ),
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )
  }

  def checkAnswersRow(
                       waypoints: Waypoints,
                       answers: UserAnswers,
                       sourcePage: CheckAnswersPage
                     )(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(AllEuDetailsQuery).map { euDetails =>

      val value = euDetails.map { details =>
          HtmlFormat.escape(details.euCountry.name)
      }.mkString("<br/>")

      SummaryListRowViewModel(
        key = "euDetails.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", AddEuDetailsPage().changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("euDetails.change.hidden"))
        )
      )
    }
  }
}
