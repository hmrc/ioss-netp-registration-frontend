package controllers.previousRegistrations

import models.{Index, UserAnswers}
import models.previousRegistrations.NonCompliantDetails
import queries.previousRegistrations.NonCompliantDetailsQuery
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait PreviousNonComplianceAnswers {

  def setNonCompliantDetailsAnswers(
                                             countryIndex: Index,
                                             schemeIndex: Index,
                                             maybeNonCompliantDetails: Option[NonCompliantDetails],
                                             updatedAnswers: UserAnswers
                                           ): Future[UserAnswers] = {
    maybeNonCompliantDetails match {
      case Some(nonCompliantDetails) =>
        Future.fromTry(updatedAnswers.set(NonCompliantDetailsQuery(countryIndex, schemeIndex), nonCompliantDetails))
      case _ =>
        updatedAnswers.toFuture
    }
  }

}
