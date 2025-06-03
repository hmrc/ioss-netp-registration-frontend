package viewmodels

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.website.DeleteWebsitePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DeleteWebsiteSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeleteWebsitePage).map {
      answer =>

        val value = if (answer) "site.yes" else "site.no"

        SummaryListRowViewModel(
          key     = "deleteWebsite.checkYourAnswersLabel",
          value   = ValueViewModel(value),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DeleteWebsiteController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("deleteWebsite.change.hidden"))
          )
        )
    }
}
