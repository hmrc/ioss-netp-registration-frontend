package viewmodels.checkAnswers.vatEuDetails

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.vatEuDetails.EuVatNumberPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object EuVatNumberSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EuVatNumberPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "euVatNumber.checkYourAnswersLabel",
          value   = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.EuVatNumberController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("euVatNumber.change.hidden"))
          )
        )
    }
}
