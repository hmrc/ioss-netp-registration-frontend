package viewmodels.checkAnswers.vatEuDetails

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.vatEuDetails.RegistrationTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RegistrationTypeSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegistrationTypePage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"registrationType.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "registrationType.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.RegistrationTypeController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("registrationType.change.hidden"))
          )
        )
    }
}
