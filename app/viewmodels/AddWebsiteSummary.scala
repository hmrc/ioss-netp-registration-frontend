package viewmodels

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.website.AddWebsitePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object AddWebsiteSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AddWebsitePage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "addWebsite.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", routes.AddWebsiteController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("addWebsite.change.hidden"))
          )
        )
    }
}
