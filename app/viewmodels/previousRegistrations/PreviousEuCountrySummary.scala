package viewmodels.previousRegistrations

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.previousRegistrations.PreviousEuCountryPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PreviousEuCountrySummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(PreviousEuCountryPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"previousEuCountry.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "previousEuCountry.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.PreviousEuCountryController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("previousEuCountry.change.hidden"))
          )
        )
    }
}
