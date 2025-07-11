/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import models.{Country, Index, PreviousSchemeType}
import models.previousRegistrations.PreviousRegistrationDetailsWithOptionalVatNumber
import models.requests.DataRequest
import pages.*
import pages.previousRegistrations.{PreviousSchemeTypePage, PreviouslyRegisteredPage}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.previousRegistrations.AllPreviousRegistrationsWithOptionalVatNumberQuery

object PreviousRegistrationsCompletionChecks extends CompletionChecks {

  def isPreviouslyRegisteredDefined(implicit request: DataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(PreviouslyRegisteredPage).exists {
      case true => request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).isDefined
      case false => request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).getOrElse(List.empty).isEmpty
    }
  }
  
  def emptyPreviousRegistrationRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    if (!isPreviouslyRegisteredDefined()) {
      Some(Redirect(controllers.previousRegistrations.routes.PreviouslyRegisteredController.onPageLoad(waypoints)))
    } else {
      None
    }
  }

  def firstIndexedIncompleteRegisteredCountry(incompleteCountries: Seq[Country])
                                               (implicit request: DataRequest[AnyContent]):
  Option[(PreviousRegistrationDetailsWithOptionalVatNumber, Int)] = {
    request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.previousEuCountry))
  }

  def getAllIncompleteRegistrationDetails()(implicit request: DataRequest[AnyContent]): Seq[PreviousRegistrationDetailsWithOptionalVatNumber] = {
    request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).map(
      _.filter(scheme =>
      scheme.previousSchemesDetails.isEmpty || scheme.previousSchemesDetails.getOrElse(List.empty).exists(_.previousSchemeNumbers.isEmpty))
    ).getOrElse(List.empty)
  }

  def incompletePreviousRegistrationRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    firstIndexedIncompleteRegisteredCountry(getAllIncompleteRegistrationDetails().map(_.previousEuCountry)) match {
      case Some(incompleteCountry) if incompleteCountry._1.previousSchemesDetails.isDefined =>
        incompleteCountry._1.previousSchemesDetails.getOrElse(List.empty).zipWithIndex.find(_._1.previousSchemeNumbers.isEmpty) match {
          case Some(schemeDetails) =>
            request.userAnswers.get(PreviousSchemeTypePage(Index(incompleteCountry._2), Index(schemeDetails._2))) match {
              case Some(PreviousSchemeType.OSS) =>
                Some(Redirect(controllers.previousRegistrations.routes.PreviousOssNumberController.onPageLoad(
                  waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
              case Some(PreviousSchemeType.IOSS) =>
                Some(Redirect(controllers.previousRegistrations.routes.PreviousIossNumberController.onPageLoad(
                  waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
              case None => None
            }
          case None => None
        }
      case Some(incompleteCountry) =>
        Some(Redirect(controllers.previousRegistrations.routes.PreviousSchemeController.onPageLoad(
          waypoints, Index(incompleteCountry._2), Index(0))))

      case None => None

    }
  }
}
