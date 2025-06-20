package viewmodels.previousRegistrations

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.AddPreviousRegistrationPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddPreviousRegistrationSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AddPreviousRegistrationPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"addPreviousRegistration.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "addPreviousRegistration.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.AddPreviousRegistrationController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("addPreviousRegistration.change.hidden"))
          )
        )
    }
}
