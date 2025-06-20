package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.EuVatNumberPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

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
