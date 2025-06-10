package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait PreviousEuCountry

object PreviousEuCountry extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with PreviousEuCountry
  case object Option2 extends WithName("option2") with PreviousEuCountry

  val values: Seq[PreviousEuCountry] = Seq(
    Option1, Option2
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"previousEuCountry.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[PreviousEuCountry] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
