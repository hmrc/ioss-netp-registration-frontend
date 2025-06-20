package viewmodels.previousRegistrations

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.DeletePreviousRegistrationPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DeletePreviousRegistrationSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeletePreviousRegistrationPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"deletePreviousRegistration.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "deletePreviousRegistration.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.DeletePreviousRegistrationController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("deletePreviousRegistration.change.hidden"))
          )
        )
    }
}
