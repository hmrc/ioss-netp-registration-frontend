package viewmodels.checkAnswers.vatEuDetails

import controllers.routes
import models.{CheckMode, Index, UserAnswers}
import pages.{CheckAnswersPage, Waypoints}
import pages.vatEuDetails.RegistrationTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object RegistrationTypeSummary {

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           countryIndex: Index,
           sourcePage: CheckAnswersPage
         )(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(RegistrationTypePage(countryIndex)).map { answer =>

      val value = ValueViewModel(
        HtmlContent(
          HtmlFormat.escape(messages(s"registrationType.$answer"))
        )
      )

      SummaryListRowViewModel(
        key = "registrationType.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", RegistrationTypePage(countryIndex).changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("registrationType.change.hidden"))
        )
      )
    }
  }
}
