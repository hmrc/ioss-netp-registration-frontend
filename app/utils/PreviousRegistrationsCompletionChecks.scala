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

import controllers.previousRegistrations.routes
import models.previousRegistrations.{PreviousRegistrationDetailsWithOptionalVatNumber, SchemeDetailsWithOptionalVatNumber}
import models.requests.DataRequest
import models.{Country, Index, PreviousScheme, PreviousSchemeType}
import pages.*
import pages.previousRegistrations.{ClientHasIntermediaryPage, PreviousSchemeTypePage, PreviouslyRegisteredPage}
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
      Some(Redirect(routes.PreviouslyRegisteredController.onPageLoad(waypoints)))
    } else {
      None
    }
  }

  private def firstIndexedIncompleteRegisteredCountry(incompleteCountries: Seq[Country])
                                                     (implicit request: DataRequest[AnyContent]):
  Option[(PreviousRegistrationDetailsWithOptionalVatNumber, Int)] = {
    request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.previousEuCountry))
  }

  def getAllIncompleteRegistrationDetails()(implicit request: DataRequest[AnyContent]): Seq[PreviousRegistrationDetailsWithOptionalVatNumber] = {
    request.userAnswers.get(AllPreviousRegistrationsWithOptionalVatNumberQuery).map(
      _.filter { scheme =>
        scheme.previousSchemesDetails.isEmpty ||
          checkIncompleteForPreviousSchemeType(scheme.previousSchemesDetails)
      }
    ).getOrElse(List.empty)
  }

  private def checkIncompleteForPreviousSchemeType(schemeDetails: Option[List[SchemeDetailsWithOptionalVatNumber]]) = {
    schemeDetails.getOrElse(List.empty).exists { schemeDetailsWithOptionalVatNumber =>
      schemeDetailsWithOptionalVatNumber.previousScheme match {
        case Some(PreviousScheme.IOSSWI | PreviousScheme.IOSSWOI) =>
          schemeDetailsWithOptionalVatNumber.clientHasIntermediary.isEmpty || schemeDetailsWithOptionalVatNumber.previousSchemeNumbers.isEmpty
        case _ =>
          schemeDetailsWithOptionalVatNumber.previousSchemeNumbers.isEmpty
      }
    }
  }

  def incompletePreviousRegistrationRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    def incompleteSchemeDetailsRedirect(
                                         incompleteCountry: (PreviousRegistrationDetailsWithOptionalVatNumber, Int),
                                         schemeDetails: (SchemeDetailsWithOptionalVatNumber, Int)
                                       ) = {
      request.userAnswers.get(PreviousSchemeTypePage(Index(incompleteCountry._2), Index(schemeDetails._2))) match {
        case Some(PreviousSchemeType.OSS) =>
          Some(Redirect(routes.PreviousOssNumberController.onPageLoad(
            waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
        case Some(PreviousSchemeType.IOSS) =>
          if (!clientHasIntermediary(Index(incompleteCountry._2), Index(schemeDetails._2))) {
            Some(Redirect(routes.ClientHasIntermediaryController.onPageLoad(waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
          } else {
            Some(Redirect(routes.PreviousIossNumberController.onPageLoad(
              waypoints, Index(incompleteCountry._2), Index(schemeDetails._2))))
          }
        case None => None
      }
    }

    firstIndexedIncompleteRegisteredCountry(getAllIncompleteRegistrationDetails().map(_.previousEuCountry)) match {
      case Some(incompleteCountry) if incompleteCountry._1.previousSchemesDetails.isDefined =>
        incompleteCountry._1.previousSchemesDetails.getOrElse(List.empty).zipWithIndex.find {
          case (schemeDetails, schemeIndex) =>
            val countryIndex = incompleteCountry._2
            val schemeType = request.userAnswers.get(
              PreviousSchemeTypePage(Index(countryIndex), Index(schemeIndex))
            )

            schemeDetails.previousSchemeNumbers.isEmpty || (schemeType.contains(PreviousSchemeType.IOSS) && schemeDetails.clientHasIntermediary.isEmpty)
        } match {
            case Some(schemeDetails) => incompleteSchemeDetailsRedirect(incompleteCountry, schemeDetails)
            case None => None
        }
      case Some(incompleteCountry) =>
        Some(Redirect(routes.PreviousSchemeController.onPageLoad(
          waypoints, Index(incompleteCountry._2), Index(0))))

      case None => None
    }
  }

  private def clientHasIntermediary(countryIndex: Index, schemeIndex: Index)(implicit request: DataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(ClientHasIntermediaryPage(countryIndex, schemeIndex)).isDefined
  }
}
