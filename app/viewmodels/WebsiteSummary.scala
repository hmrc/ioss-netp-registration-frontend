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

package viewmodels

import models.{Index, UserAnswers}
import pages.{CheckAnswersPage, Waypoints}
import pages.website.{AddWebsitePage, DeleteWebsitePage, WebsitePage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.AllWebsites
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object WebsiteSummary  {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddWebsitePage): Seq[ListItemWrapper] =
    answers.get(AllWebsites).getOrElse(List.empty).zipWithIndex.map {
      case (website, index) =>
      ListItemWrapper(
        ListItem(
          name = HtmlFormat.escape(website.site).toString,
          changeUrl = WebsitePage(Index(index)).changeLink(waypoints, sourcePage).url,
          removeUrl = DeleteWebsitePage(Index(index)).route(waypoints).url
        ),
        removeButtonEnabled = true
      )
    }

  def checkAnswersRow(
                       waypoints: Waypoints,
                       answers: UserAnswers,
                       sourcePage: CheckAnswersPage
                     )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllWebsites).map {
      websites =>

        val value = websites.map {
          website =>
            HtmlFormat.escape(website.site)
        }.mkString("<br/>")

        val addWebsitePageChangeUrl = AddWebsitePage().changeLink(waypoints, sourcePage).url

        val listRowViewModel = SummaryListRowViewModel(
          key = "website.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", addWebsitePageChangeUrl)
              .withVisuallyHiddenText(messages("website.change.hidden"))
          )
        )

        listRowViewModel
    }
}
