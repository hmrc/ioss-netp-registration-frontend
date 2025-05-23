package viewmodels

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.website.WebsitePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object WebsiteSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(WebsitePage).map {
      answer =>

        SummaryListRowViewModel(
          key     = "website.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlFormat.escape(answer).toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.WebsiteController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("website.change.hidden"))
          )
        )
    }
}
