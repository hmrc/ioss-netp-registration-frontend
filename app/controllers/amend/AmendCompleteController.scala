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

package controllers.amend

import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.domain.PreviousSchemeDetails
import models.{Country, TradingName, UserAnswers, Website}
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails}
import models.requests.AuthenticatedMandatoryRegistrationRequest
import models.vatEuDetails.EuDetails
import pages.{BusinessContactDetailsPage, JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import queries.{AllWebsites, OriginalRegistrationQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.AmendCompleteView
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.BusinessContactDetailsSummary
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}

import javax.inject.Inject
import scala.util.{Failure, Success}

class AmendCompleteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         frontendAppConfig: FrontendAppConfig,
                                         view: AmendCompleteView
                                       ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndRequireRegistration(inAmend = true).async {
    implicit request =>
      
      request.userAnswers.get(OriginalRegistrationQuery(request.iossNumber)) match {
        case Some(originalRegistration) =>
          val amendedList: SummaryList = detailsList(originalRegistration)
          Ok(view(frontendAppConfig.feedbackUrl, frontendAppConfig.intermediaryYourAccountUrl, amendedList)).toFuture
        case None =>
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
      }
  }

  private def detailsList(originalRegistration: EtmpDisplayRegistration)(implicit request: AuthenticatedMandatoryRegistrationRequest[AnyContent]) =
    SummaryListViewModel(
      rows = (
        getHasTradingNameRows(originalRegistration) ++
          getTradingNameRows(originalRegistration) ++
          getHasPreviouslyRegistered(originalRegistration) ++
          getPreviouslyRegisteredRows(originalRegistration) ++
          getHasFixedEstablishmentInEuDetails(originalRegistration.schemeDetails) ++
          getFixedEstablishmentInEuRows(originalRegistration.schemeDetails) ++
          getAmendedFixedEstablishmentInEuRows(originalRegistration.schemeDetails) ++
          getCountriesWithNewSchemes(originalRegistration) ++
          getWebsitesRows(originalRegistration) ++
          getBusinessContactDetailsRows(originalRegistration)
      ).flatten
    )

  private def getHasTradingNameRows(originalRegistration: EtmpDisplayRegistration)
                                   (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty)
    val hasChangedToNo = amendedAnswers.isEmpty && originalAnswers.nonEmpty
    val hasChangedToYes = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || originalAnswers.isEmpty
    val notAmended = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || amendedAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo || hasChangedToYes) {
      Seq(HasTradingNameSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(originalRegistration: EtmpDisplayRegistration)(implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] =
    val originalAnswers = originalRegistration.tradingNames.map(_.tradingName)
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(List.empty)
    val addedTradingName = amendedAnswers.diff(originalAnswers)
    val removedTradingNames = originalAnswers.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => TradingName(amended)
    } ++ amendedAnswers.drop(originalAnswers.size).map(tradingName => TradingName(tradingName))

    val addedTradingNameRow = if (addedTradingName.nonEmpty) {
      request.userAnswers.set(AllTradingNamesQuery, changedTradingName) match {
        case Success(amendedUserAnswer) =>
          Some(TradingNameSummary.amendedAnswersRow(amendedUserAnswer))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedTradingNameRow = Some(TradingNameSummary.removedAnswersRow(removedTradingNames))

    Seq(addedTradingNameRow, removedTradingNameRow).flatten

  private def getHasPreviouslyRegistered(originalRegistration: EtmpDisplayRegistration)
                                        (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] =

    val originalAnswers = originalRegistration.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
    val amendedAnswers = request.userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(List.empty)
    val hasChangedToNo = amendedAnswers.diff(originalAnswers)
    val hasChangedToYes = originalAnswers.diff(amendedAnswers)
    val notAmended = amendedAnswers.nonEmpty && originalAnswers.nonEmpty || amendedAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo.nonEmpty || hasChangedToYes.nonEmpty) {
      Seq(PreviouslyRegisteredSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }

  private def getPreviouslyRegisteredRows(originalRegistration: EtmpDisplayRegistration)
                                         (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] =

    val originalAnswers = originalRegistration.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
    val amendedAnswers = request.userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(List.empty)

    val newPreviouslyRegisteredCountry = amendedAnswers.filterNot { addedCountry =>
      originalAnswers.contains(addedCountry)
    }

    if (newPreviouslyRegisteredCountry.nonEmpty) {
      val addedDetails = request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(List.empty)
        .filter(details => newPreviouslyRegisteredCountry.contains(details.previousEuCountry.code))

      request.userAnswers.set(AllPreviousRegistrationsQuery, addedDetails) match {
        case Success(amendedUserAnswers) =>
          Seq(PreviousRegistrationSummary.amendedAnswersRow(answers = amendedUserAnswers))
        case Failure(exception) =>
          Seq.empty
      }
    } else {
      Seq.empty
    }


  private def getHasFixedEstablishmentInEuDetails(originalRegistration: EtmpDisplaySchemeDetails)
                                                 (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] =

    val originalCountries: Seq[String] = originalRegistration.euRegistrationDetails.map(_.issuedBy)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(Seq.empty)
    val hasChangedToNo: Boolean = amendedCountries.diff(originalCountries).nonEmpty
    val hasChangedToYes: Boolean = originalCountries.diff(amendedCountries).nonEmpty
    val notAmended: Boolean = originalCountries.nonEmpty && amendedCountries.nonEmpty || originalCountries.isEmpty && amendedCountries.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToYes || hasChangedToNo) {
      Seq(HasFixedEstablishmentSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }

  private def getFixedEstablishmentInEuRows(originalAnswers: EtmpDisplaySchemeDetails)
                                           (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalCountries: Seq[String] = originalAnswers.euRegistrationDetails.map(_.issuedBy)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllEuDetailsQuery)
      .map(_.map(_.euCountry.code))
      .getOrElse(Seq.empty)

    val addedFixedEstablishmentDetails: Seq[String] = amendedCountries.diff(originalCountries)
    val removedFixedEstablishmentDetails: Seq[String] = originalCountries.diff(amendedCountries)

    val newFixedEstablishmentDetails: Seq[String] = amendedCountries.filterNot { amendedCountryCode =>
      originalCountries.contains(amendedCountryCode)
    }

    val addedFixedEstablishmentRow = if (addedFixedEstablishmentDetails.nonEmpty) {
      val amendedFixedEstablishmentDetails = request.userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty)
        .filter(fixedEstablishmentDetails => newFixedEstablishmentDetails
          .contains(fixedEstablishmentDetails.euCountry.code)
        ).toList

      request.userAnswers.set(AllEuDetailsQuery, amendedFixedEstablishmentDetails) match {
        case Success(amendedAnswers) =>
          Some(EuDetailsSummary.addedRow(amendedAnswers))

        case Failure(_) => None
      }
    } else {
      None
    }

    val removedFixedEstablishmentCountries: Seq[Country] = removedFixedEstablishmentDetails.flatMap(Country.fromCountryCode)

    val removedFixedEstablishmentDetailsRow = Some(EuDetailsSummary.removedRow(removedFixedEstablishmentCountries))

    Seq(addedFixedEstablishmentRow, removedFixedEstablishmentDetailsRow).flatten
  }

  private def getAmendedFixedEstablishmentInEuRows(originalRegistration: EtmpDisplaySchemeDetails)
                                                  (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] = {

    val allFixedEstablishmentDetails = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

    val changedFixedEstablishmentCountries: Seq[Country] = allFixedEstablishmentDetails.flatMap { fixedEstablishmentDetails =>
      originalRegistration.euRegistrationDetails.find(_.issuedBy == fixedEstablishmentDetails.euCountry.code) match {
        case Some(originalFixedEstablishmentDetails)
          if hasFixedEstablishmentDetailsChanged(fixedEstablishmentDetails, originalFixedEstablishmentDetails) =>
          Some(fixedEstablishmentDetails.euCountry)

        case _ =>
          None
      }
    }

    if (changedFixedEstablishmentCountries.nonEmpty) {
      Seq(EuDetailsSummary.amendedRow(changedFixedEstablishmentCountries))
    } else {
      Seq.empty
    }
  }

  private def hasFixedEstablishmentDetailsChanged(amendedDetails: EuDetails, originalDetails: EtmpDisplayEuRegistrationDetails): Boolean = {

    val vatNumberWithoutCountryCode: Option[String] = amendedDetails.euVatNumber.map(_.stripPrefix(amendedDetails.euCountry.code))
    val originalRegistrationVatNumber: Option[String] = originalDetails.vatNumber

    amendedDetails.tradingNameAndBusinessAddress.map(_.tradingName.name).exists(_ != originalDetails.fixedEstablishmentTradingName) ||
      amendedDetails.tradingNameAndBusinessAddress.map(_.address).exists(address =>
        !originalDetails.fixedEstablishmentAddressLine1.equals(address.line1) ||
          !originalDetails.fixedEstablishmentAddressLine2.equals(address.line2) ||
          !originalDetails.townOrCity.equals(address.townOrCity) ||
          !originalDetails.regionOrState.equals(address.stateOrRegion) ||
          !originalDetails.postcode.equals(address.postCode)
      ) ||
      !vatNumberWithoutCountryCode.equals(originalRegistrationVatNumber) ||
      !amendedDetails.euTaxReference.equals(originalDetails.taxIdentificationNumber)
  }

  private def getCountriesWithNewSchemes(originalRegistration: EtmpDisplayRegistration)
                                        (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] =

    val amendedDetails = request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(List.empty)
    val registrationDetails = originalRegistration.schemeDetails.previousEURegistrationDetails

    val changedSchemeDetails = amendedDetails.flatMap { amendedCountry =>
      val matchingEuCountry = registrationDetails.filter(_.issuedBy == amendedCountry.previousEuCountry.code)
      val existingSchemeDetails = matchingEuCountry.map { registration =>
        PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registration).previousSchemeNumbers
      }
      val newSchemes = amendedCountry.previousSchemesDetails.map(_.previousSchemeNumbers)

      val hasSchemeNumbersChanged = existingSchemeDetails.nonEmpty && newSchemes != existingSchemeDetails

      if (hasSchemeNumbersChanged) {
        Some(amendedCountry.previousEuCountry)
      } else {
        None
      }
    }

    if (changedSchemeDetails.nonEmpty) {
      Seq(PreviousRegistrationSummary.changedAnswersRow(changedSchemeDetails))
    } else {
      Seq.empty
    }


  private def getWebsitesRows(originalRegistration: EtmpDisplayRegistration)
                             (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.schemeDetails.websites.map(_.websiteAddress)
    val amendedUA = request.userAnswers.get(AllWebsites).map(_.map(_.site)).getOrElse(List.empty)
    val addedWebsites = amendedUA.diff(originalAnswers)
    val removedWebsites = originalAnswers.diff(amendedUA)

    val changedWebsiteAnswers: List[Website] = amendedUA.zip(originalAnswers).collect {
      case (amended, original) if amended != original => Website(amended)
    } ++ amendedUA.drop(originalAnswers.size).map(site => Website(site))

    val addedWebsiteRow = if (addedWebsites.nonEmpty) {
      request.userAnswers.set(AllWebsites, changedWebsiteAnswers) match {
        case Success(amendedUserAnswers) =>
          Some(WebsiteSummary.amendedAnswersRow(amendedUserAnswers))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedWebsiteRow = Some(WebsiteSummary.removedAnswersRow(removedWebsites))
    Seq(addedWebsiteRow, removedWebsiteRow).flatten
  }

  private def getBusinessContactDetailsRows(originalRegistration: EtmpDisplayRegistration)
                                           (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalContactName = originalRegistration.schemeDetails.contactName
    val originalTelephone = originalRegistration.schemeDetails.businessTelephoneNumber
    val originalEmail = originalRegistration.schemeDetails.businessEmailId
    val amendedUA = request.userAnswers.get(BusinessContactDetailsPage)

    Seq(
      if (!amendedUA.map(_.fullName).contains(originalContactName)) {
        BusinessContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.telephoneNumber).contains(originalTelephone)) {
        BusinessContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.emailAddress).contains(originalEmail)) {
        BusinessContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }
}
