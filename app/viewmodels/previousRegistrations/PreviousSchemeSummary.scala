package viewmodels.previousRegistrations

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.PreviousSchemePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PreviousSchemeSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PreviousSchemePage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"previousScheme.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "previousScheme.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.PreviousSchemeController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("previousScheme.change.hidden"))
          )
        )
    }
}
