package models

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait RegistrationType

object RegistrationType extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with RegistrationType
  case object Option2 extends WithName("option2") with RegistrationType

  val values: Seq[RegistrationType] = Seq(
    Option1, Option2
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"registrationType.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[RegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
