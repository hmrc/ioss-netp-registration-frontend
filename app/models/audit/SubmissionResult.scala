package models.audit

import models.{Enumerable, WithName}

sealed trait SubmissionResult

object SubmissionResult extends Enumerable.Implicits {

  case object Success extends WithName("success") with SubmissionResult

  case object Failure extends WithName("failure") with SubmissionResult

  val values: Seq[SubmissionResult] = Seq(Success, Failure)

  implicit val enumerable: Enumerable[SubmissionResult] =
    Enumerable(values.map(v => v.toString -> v): _*)
}