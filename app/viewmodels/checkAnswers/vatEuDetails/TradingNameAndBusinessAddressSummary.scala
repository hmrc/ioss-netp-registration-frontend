package viewmodels.checkAnswers.vatEuDetails

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.vatEuDetails.TradingNameAndBusinessAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object TradingNameAndBusinessAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TradingNameAndBusinessAddressPage).map {
      answer =>

      val value = HtmlFormat.escape(answer.field1).toString + "<br/>" + HtmlFormat.escape(answer.field2).toString

        SummaryListRowViewModel(
          key     = "tradingNameAndBusinessAddress.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.TradingNameAndBusinessAddressController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("tradingNameAndBusinessAddress.change.hidden"))
          )
        )
    }
}
