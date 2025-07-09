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

import models.{Country, CountryWithValidationDetails, Index}
import models.euDetails.EuDetails
import models.RegistrationType.{TaxId, VatNumber}
import models.requests.DataRequest
import pages.Waypoints
import pages.vatEuDetails.*
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Result}
import queries.euDetails.{AllEuDetailsQuery, EuDetailsQuery}

object EuDetailsCompletionChecks {

  private val query = AllEuDetailsQuery

  def isEuDetailsDefined()(implicit requests: DataRequest[AnyContent]): Boolean = {
    requests.userAnswers.get(HasFixedEstablishmentPage).exists {
      case true => requests.userAnswers.get(query).exists(_.nonEmpty)
      case false => requests.userAnswers.get(query).getOrElse(List.empty).isEmpty
    }
  }

  def emptyEuDetailsRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if(!isEuDetailsDefined()) {
    Some(Redirect(HasFixedEstablishmentPage.route(waypoints).url))
  } else {
    None
  }
  
  def incompleteEuDetailsRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    firstIndexedIncompleteEuDetails(getAllIncompleteEuDetails().map(_.euCountry)).flatMap {
      case (euDetails: EuDetails, index: Int) =>
        val countryIndex: Index = Index(index)
        incompleteCheckEuDetailsRedirect(waypoints, countryIndex, euDetails)
    }
  }
  
  def getIncompleteEuDetails(countryIndex: Index)(implicit request: DataRequest[AnyContent]): Option[EuDetails] = {
    request.userAnswers.get(EuDetailsQuery(countryIndex)).filter { euDetails =>
      sellsGoodsToEuConsumersMethod(euDetails) || checkVatNumber(euDetails)
    }
  }
  
  def getAllIncompleteEuDetails()(implicit request: DataRequest[AnyContent]): Seq[EuDetails] = {
    request.userAnswers.get(query).map(_.filter { euDetails =>
      sellsGoodsToEuConsumersMethod(euDetails) || checkVatNumber(euDetails)
    }).getOrElse(List.empty)
  }
  
  private def sellsGoodsToEuConsumersMethod(euDetails: EuDetails): Boolean = {
    euDetails.registrationType.isEmpty ||
      (euDetails.registrationType.contains(VatNumber) && euDetails.euVatNumber.isEmpty) ||
      (euDetails.registrationType.contains(TaxId) && euDetails.euTaxReference.isEmpty)
  }
  
  private def firstIndexedIncompleteEuDetails(incompleteCountries: Seq[Country])(implicit request: DataRequest[AnyContent]): Option[(EuDetails, Int)] = {
    request.userAnswers.get(query)
      .getOrElse(List.empty)
      .zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.euCountry))
  }

  private def checkVatNumber(euDetails: EuDetails): Boolean = {
    euDetails.euVatNumber.exists { euVatNumber =>
      CountryWithValidationDetails.euCountriesWithVRNValidationRules.find(_.country.code == euDetails.euCountry.code) match {
        case Some(validationRule) =>
          !euVatNumber.matches(validationRule.vrnRegex)
        case _ => true
      }
    }
  }
  
  private def incompleteCheckEuDetailsRedirect(waypoints: Waypoints, countryIndex: Index, euDetails: EuDetails): Option[Result] = {
    val redirectCalls: Seq[(Boolean, Call)] = Seq(
      euDetails.euCountry.name.isEmpty ->
        EuCountryPage(countryIndex).route(waypoints),

      euDetails.tradingNameAndBusinessAddress.isEmpty ->
        TradingNameAndBusinessAddressPage(countryIndex).route(waypoints),

      euDetails.registrationType.isEmpty ->
        RegistrationTypePage(countryIndex).route(waypoints),

      (euDetails.registrationType.contains(VatNumber) && (euDetails.euVatNumber.isEmpty || checkVatNumber(euDetails))) ->
        EuVatNumberPage(countryIndex).route(waypoints),

      (euDetails.registrationType.contains(TaxId) && euDetails.euTaxReference.isEmpty) ->
        EuTaxReferencePage(countryIndex).route(waypoints),
    )

    redirectCalls.find(_._1).map { case (_, redirectCall) =>
      Redirect(redirectCall)
    }
  }
}
