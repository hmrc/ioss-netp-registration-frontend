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
import pages.{BusinessContactDetailsPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object BusinessContactDetailsSummary {

  def rowFullName(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(BusinessContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.fullName).toString

      SummaryListRowViewModel(
        key = "businessContactDetails.fullName",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", BusinessContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("businessContactDetails.change.fullName.hidden"))
        )
      )
    }
  }

  def rowTelephoneNumber(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(BusinessContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.telephoneNumber).toString

      SummaryListRowViewModel(
        key = "businessContactDetails.telephoneNumber",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", BusinessContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("businessContactDetails.change.telephoneNumber.hidden"))
        )
      )
    }
  }

  def rowEmailAddress(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(BusinessContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.emailAddress).toString

      SummaryListRowViewModel(
        key = "businessContactDetails.emailAddress",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", BusinessContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("businessContactDetails.change.emailAddress.hidden"))
        )
      )
    }
  }
}
