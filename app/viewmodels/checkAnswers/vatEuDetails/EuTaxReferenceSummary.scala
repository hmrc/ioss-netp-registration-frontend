package viewmodels.checkAnswers.vatEuDetails

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.vatEuDetails.EuTaxReferencePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object EuTaxReferenceSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EuTaxReferencePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "euTaxReference.checkYourAnswersLabel",
          value   = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.EuTaxReferenceController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("euTaxReference.change.hidden"))
          )
        )
    }
}
