package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.UpdateClientEmailAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object UpdateClientEmailAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(UpdateClientEmailAddressPage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "updateClientEmailAddress.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.UpdateClientEmailAddressController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("updateClientEmailAddress.change.hidden"))
          )
        )
    }
}
