/*
 * Copyright 2026 HM Revenue & Customs
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

package viewmodels.checkAnswers.tradingNames

import models.{Index, UserAnswers}
import pages.tradingNames.{AddTradingNamePage, DeleteTradingNamePage, TradingNamePage}
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.{ListItem, ListItemWrapper}

object TradingNameSummary {

  def addToListRows(waypoints: Waypoints, answers: UserAnswers, sourcePage: AddItemPage): Seq[ListItemWrapper] =
    answers.get(AllTradingNamesQuery).getOrElse(List.empty).zipWithIndex.map {
      case (tradingName, index) =>
        ListItemWrapper(
          ListItem(
            name = HtmlFormat.escape(tradingName.name).toString,
            changeUrl = TradingNamePage(Index(index)).changeLink(waypoints, sourcePage).url,
            removeUrl = DeleteTradingNamePage(Index(index)).route(waypoints).url
          ),
          removeButtonEnabled = true
        )
    }


  def checkAnswersRow(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)
                     (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllTradingNamesQuery).map {
      tradingNames =>

        val value = tradingNames.map {
          name =>
            HtmlFormat.escape(name.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = "tradingName.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", AddTradingNamePage().changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("tradingName.change.hidden"))
          )
        )
    }

  def checkAnswersRowWithoutAction(waypoints: Waypoints, answers: UserAnswers)
                     (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllTradingNamesQuery).map {
      tradingNames =>

        val value = tradingNames.map {
          name =>
            HtmlFormat.escape(name.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = "tradingName.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def amendedAnswersRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllTradingNamesQuery).map {
      tradingNames =>

        val value = tradingNames.map {
          name =>
            HtmlFormat.escape(name.name)
        }.mkString("<br/>")

        SummaryListRowViewModel(
          key = KeyViewModel("tradingName.checkYourAnswersLabel.added").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
    }

  def removedAnswersRow(removedTradingNames: Seq[String])(implicit messages: Messages): Option[SummaryListRow] =

    if (removedTradingNames.nonEmpty) {
      val value = removedTradingNames.map {
        name =>
          HtmlFormat.escape(name)
      }.mkString("<br/>")

      Some(
        SummaryListRowViewModel(
          key = KeyViewModel("tradingName.checkYourAnswersLabel.removed").withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(value))
        )
      )
    } else {
      None
    }
}