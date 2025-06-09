package models.euDetails

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait RegistrationType

object RegistrationType extends Enumerable.Implicits {

  case object VatNumber extends WithName("vatNumber") with RegistrationType

  case object TaxId extends WithName("taxId") with RegistrationType

  val values: Seq[RegistrationType] = Seq(
    VatNumber, TaxId
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"registrationType.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[RegistrationType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
