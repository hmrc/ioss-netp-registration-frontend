package viewmodels.previousRegistrations

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.PreviouslyRegisteredPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PreviouslyRegisteredSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PreviouslyRegisteredPage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "previouslyRegistered.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", routes.PreviouslyRegisteredController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("previouslyRegistered.change.hidden"))
          )
        )
    }
}
