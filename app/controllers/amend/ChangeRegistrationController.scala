/*
 * Copyright 2026 HM Revenue & Customs
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

import config.Constants.ukCountryCodeAreaPrefix
import config.FrontendAppConfig
import controllers.GetClientCompanyName
import controllers.actions.AuthenticatedControllerComponents
import formats.Format.dateFormatter
import logging.Logging
import models.audit.NetpAmendRegistrationAuditModel
import models.audit.RegistrationAuditType.AmendRegistration
import models.audit.SubmissionResult.{Failure, Success}
import models.domain.{PreviousRegistration, PreviousSchemeDetails}
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.etmp.{EtmpExclusion, EtmpPreviousEuRegistrationDetails}
import models.requests.AuthenticatedMandatoryRegistrationRequest
import models.vatEuDetails.EuDetails
import models.{CheckMode, Country, InternationalAddress, UserAnswers}
import pages.*
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{AllWebsites, OriginalRegistrationQuery}
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import utils.VatInfoCompletionChecks.*
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.tradingNames.*
import viewmodels.checkAnswers.vatEuDetails.*
import viewmodels.govuk.summarylist.*
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}
import views.html.ChangeRegistrationView

import javax.inject.Inject
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ChangeRegistrationController @Inject()(
                                              cc: AuthenticatedControllerComponents,
                                              view: ChangeRegistrationView,
                                              registrationService: RegistrationService,
                                              auditService: AuditService,
                                              frontendAppConfig: FrontendAppConfig
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndRequireRegistration(inAmend = true).async {
    implicit request =>

      val thisPage = ChangeRegistrationPage

      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))

      val clientBasedInUk = request.userAnswers.get(BusinessBasedInUKPage).getOrElse(false)
      val clientHasUkAddress: Option[Country] = request.userAnswers.get(ClientCountryBasedPage)
      val countryIsUk: Boolean = clientHasUkAddress match
        case Some(someCountry) => someCountry.code.startsWith(ukCountryCodeAreaPrefix)
        case None => false
      val hasEtmpOtherAddress: Option[InternationalAddress] = request.userAnswers.get(ClientBusinessAddressPage)

      val existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails] = request.registrationWrapper
        .etmpDisplayRegistration.schemeDetails.previousEURegistrationDetails

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.etmpDisplayRegistration.exclusions.lastOption.flatMap { exclusion =>
        exclusion.exclusionReason match {
          case models.etmp.EtmpExclusionReason.Reversal => None
          case _ => Some(exclusion)
        }
      }

      val isExcluded = maybeExclusion.isDefined

      val exclusionDeadline: Option[String] = maybeExclusion.flatMap { exclusion =>
        val deadline = exclusion.effectiveDate.minusDays(1)
        if (LocalDate.now().isBefore(deadline) || LocalDate.now().isEqual(deadline)) {
          Some(deadline.format(dateFormatter))
        } else {
          None
        }
      }

      getClientCompanyName(waypoints) { companyName =>

        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            BusinessBasedInUKSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientHasVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientVatNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            if (countryIsUk) {
              None
            } else if (isExcluded){
              ClientCountryBasedSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientCountryBasedSummary.row(waypoints, request.userAnswers, thisPage)
            },
            if (isExcluded) {
              ClientTaxReferenceSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientTaxReferenceSummary.row(waypoints, request.userAnswers, thisPage)
            },
            if (!hasEtmpOtherAddress.isDefined) {
              VatRegistrationDetailsSummary.changeRegVatBusinessNameRow(waypoints, request.userAnswers, thisPage, clientBasedInUk)
            } else if (isExcluded) {
              ClientBusinessNameSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else {
              ClientBusinessNameSummary.row(waypoints, request.userAnswers, thisPage)
            },
            ClientHasUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientUtrNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            ClientsNinoNumberSummary.rowWithoutAction(waypoints, request.userAnswers),
            if (isExcluded && hasEtmpOtherAddress.isDefined) {
              ClientBusinessAddressSummary.rowWithoutAction(waypoints, request.userAnswers)
            } else if (isExcluded) {
              VatRegistrationDetailsSummary.changeRegBusinessAddressRow(waypoints, request.userAnswers, thisPage)
            } else if (hasEtmpOtherAddress.isDefined && countryIsUk) {
              ClientBusinessAddressSummary.changeUkBasedRegRow(waypoints, request.userAnswers, thisPage)
            } else if (hasEtmpOtherAddress.isDefined && !countryIsUk) {
              ClientBusinessAddressSummary.row(waypoints, request.userAnswers, thisPage)
            } else {
              VatRegistrationDetailsSummary.changeRegBusinessAddressRow(waypoints, request.userAnswers, thisPage)
            }
          ).flatten
        )

        val (hasTradingNameRow, tradingNameRow) = getTradingNameRows(request.userAnswers, waypoints, thisPage, isExcluded)

        val (previouslyRegisteredRow, previousRegSummaryRow) = getPreviousRegRows(request.userAnswers, waypoints, thisPage, existingPreviousRegistrations, isExcluded)

        val (hasFixedEstablishmentRow, euDetailsSummaryRow) = getFixedEstablishmentRows(waypoints, request.userAnswers, thisPage, isExcluded)

        val (contactNameRow, telephoneNumRow, emailRow) = getBusinessContactRows(waypoints, request.userAnswers, thisPage)

        val websiteSummary = if (isExcluded) {
          WebsiteSummary.checkAnswersRowWithoutAction(waypoints, request.userAnswers)
        } else {
          WebsiteSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
        }

        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            hasTradingNameRow,
            tradingNameRow,
            previouslyRegisteredRow,
            previousRegSummaryRow,
            hasFixedEstablishmentRow,
            euDetailsSummaryRow,
            websiteSummary,
            contactNameRow,
            telephoneNumRow,
            emailRow
          ).flatten
        )

        val isValid: Boolean = validate()(request.request)
        val hasChanges: Boolean =
          request.userAnswers.get(OriginalRegistrationQuery(request.iossNumber)) match {
            case Some(originalRegistrationAnswers) =>
              !answersHaveChanged(originalRegistrationAnswers)

            case None =>
              true
          }

        Ok(view(waypoints, companyName, request.iossNumber, registrationDetailsList, importOneStopShopDetailsList, isValid, isExcluded, maybeExclusion, exclusionDeadline, hasChanges, frontendAppConfig.clientListUrl)).toFuture
      }(request.request)
  }


  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.identifyAndRequireRegistration(inAmend = true).async {
    implicit request =>

      if (incompletePrompt) {
        getFirstValidationErrorRedirect(waypoints)(request.request) match {
          case Some(redirectResult) => Future.successful(redirectResult)
          case None =>
            submitAmendReg()
        }
      } else {
        submitAmendReg()
      }
  }

  private def submitAmendReg()(implicit request: AuthenticatedMandatoryRegistrationRequest[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {

    registrationService.amendRegistration(
      answers = request.userAnswers,
      registration = request.registrationWrapper.etmpDisplayRegistration,
      iossNumber = request.iossNumber
    ).map {
      case Right(amendResponse) =>
        auditService.audit(
          NetpAmendRegistrationAuditModel.build(
            registrationAuditType = AmendRegistration,
            userAnswers = request.userAnswers,
            amendRegistrationResponse = Some(amendResponse),
            submissionResult = Success
          )
        )
        Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
      case Left(error) =>
        logger.error(s"Unexpected result on submit: ${error.body}")
        auditService.audit(
          NetpAmendRegistrationAuditModel.build(
            registrationAuditType = AmendRegistration,
            userAnswers = request.userAnswers,
            amendRegistrationResponse = None,
            submissionResult = Failure
          )
        )
        Redirect(controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad())
    }
  }

  private def getTradingNameRows(answers: UserAnswers, waypoints: Waypoints, changePage: ChangeRegistrationPage.type, isExcluded: Boolean)(implicit messages: Messages) = {

    if (isExcluded) {
      val maybeExcludedHasTradingNameSummaryRow = HasTradingNameSummary.rowWithoutAction(answers, waypoints)
      val excludedTradingNameSummaryRow = TradingNameSummary.checkAnswersRowWithoutAction(waypoints, answers)
      val formattedHasTradingNameSummary = maybeExcludedHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
        if (excludedTradingNameSummaryRow.nonEmpty) {
          nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasTradingNameSummaryRow
        }
      }
      (formattedHasTradingNameSummary, excludedTradingNameSummaryRow)
    } else {
      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(answers, waypoints, changePage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, changePage)

      val formattedHasTradingNameSummary = maybeHasTradingNameSummaryRow.map { nonOptHasTradingNameSummaryRow =>
        if (tradingNameSummaryRow.nonEmpty) {
          nonOptHasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasTradingNameSummaryRow
        }
      }
      (formattedHasTradingNameSummary, tradingNameSummaryRow)
    }
  }

  private def getPreviousRegRows(
                                  answers: UserAnswers,
                                  waypoints: Waypoints,
                                  currentPage: ChangeRegistrationPage.type,
                                  existingPreviousRegistrations: Seq[EtmpPreviousEuRegistrationDetails],
                                  isExcluded: Boolean
                                )(implicit messages: Messages) = {

    val convertedExistingPreviousRegistrations = PreviousRegistration.fromEtmpPreviousEuRegistrationDetails(existingPreviousRegistrations)
    if (isExcluded) {
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(answers, waypoints)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(answers, convertedExistingPreviousRegistrations, waypoints)
      val formattedPreviouslyRegistered = previouslyRegisteredSummaryRow.map { previouslyRegisteredRow =>
        if (previousRegistrationSummaryRow.nonEmpty) {
          previouslyRegisteredRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          previouslyRegisteredRow
        }
      }
      (formattedPreviouslyRegistered, previousRegistrationSummaryRow)
    } else {
      val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.row(answers, waypoints, currentPage)
      val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRow(answers, convertedExistingPreviousRegistrations, waypoints, currentPage)

      val formattedPreviouslyRegistered = previouslyRegisteredSummaryRow.map { previouslyRegisteredRow =>
        if (previousRegistrationSummaryRow.nonEmpty) {
          previouslyRegisteredRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          previouslyRegisteredRow
        }
      }

      (formattedPreviouslyRegistered, previousRegistrationSummaryRow)
    }
  }

  private def getFixedEstablishmentRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type, isExcluded: Boolean)(implicit messages: Messages) = {
    if (isExcluded) {
      val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.rowWithoutAction(waypoints, answers)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRowWithoutAction(waypoints, answers)

      val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
        if (euDetailsSummaryRow.nonEmpty) {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
        }
      }
      (formattedHasFixedEstablishmentSummaryRow, euDetailsSummaryRow)
    } else {

      val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, page)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, page)

      val formattedHasFixedEstablishmentSummaryRow = hasFixedEstablishmentSummaryRow.map { nonOptHasFixedEstablishmentSummaryRow =>
        if (euDetailsSummaryRow.nonEmpty) {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
        } else {
          nonOptHasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list")
        }
      }
      (formattedHasFixedEstablishmentSummaryRow, euDetailsSummaryRow)
    }
  }

  private def getBusinessContactRows(waypoints: Waypoints, answers: UserAnswers, page: ChangeRegistrationPage.type)(implicit messages: Messages) = {

    val formattedContactName = BusinessContactDetailsSummary.rowFullName(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedTelephoneNumber = BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, answers, page).map(_.withCssClass("govuk-summary-list__row--no-border"))
    val formattedEmailAddress = BusinessContactDetailsSummary.rowEmailAddress(waypoints, answers, page)

    (formattedContactName, formattedTelephoneNumber, formattedEmailAddress)
  }

  private def answersHaveChanged(originalAnswers: EtmpDisplayRegistration)
                                (implicit request: AuthenticatedMandatoryRegistrationRequest[_]): Boolean = {

    val countryBasedInChanged =
      request.userAnswers.get(ClientCountryBasedPage).exists { country =>
        originalAnswers.otherAddress.exists(_.issuedBy != country.code)
      }

    val taxReferenceChanged =
      request.userAnswers.get(ClientTaxReferencePage).exists { taxReference =>
        taxReference != originalAnswers.customerIdentification.idValue
      }

    val clientBusinessNameChanged =
      request.userAnswers.get(ClientBusinessNamePage).map(_.name) !=
        originalAnswers.otherAddress.flatMap(_.tradingName)

    val clientBusinessAddressChanged =
      request.userAnswers.get(ClientBusinessAddressPage) match {
        case Some(address) =>
          originalAnswers.otherAddress match {
            case Some(originalAddress) =>
              address.line1 != originalAddress.addressLine1 ||
                address.line2 != originalAddress.addressLine2 ||
                address.townOrCity != originalAddress.townOrCity ||
                address.stateOrRegion != originalAddress.regionOrState ||
                address.postCode != originalAddress.postcode

            case None =>
              true
          }

        case None =>
          originalAnswers.otherAddress.isDefined
      }

    val tradingNamesChanged =
      request.userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(Seq.empty) !=
        originalAnswers.tradingNames.map(_.tradingName)

    val previousRegistrationsChanged =
      request.userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(Seq.empty) !=
        originalAnswers.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct

    val fixedEstablishmentsChanged =
      request.userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(Seq.empty) !=
        originalAnswers.schemeDetails.euRegistrationDetails.map(_.issuedBy)

    val amendedFixedEstablishmentsChanged =
      request.userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty).exists { amendedDetails =>
        originalAnswers.schemeDetails.euRegistrationDetails
          .find(_.issuedBy == amendedDetails.euCountry.code)
          .exists(originalDetails => hasFixedEstablishmentDetailsChanged(amendedDetails, originalDetails))
      }

    val previousRegistrationSchemesChanged =
      request.userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(Seq.empty).exists { amendedCountry =>
        val matchingOriginalRegistrations =
          originalAnswers.schemeDetails.previousEURegistrationDetails
            .filter(_.issuedBy == amendedCountry.previousEuCountry.code)

        val originalSchemeNumbers =
          matchingOriginalRegistrations.map { registration =>
            PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registration).previousSchemeNumbers
          }

        val amendedSchemeNumbers =
          amendedCountry.previousSchemesDetails.map(_.previousSchemeNumbers)

        originalSchemeNumbers.nonEmpty && amendedSchemeNumbers != originalSchemeNumbers
      }

    val websitesChanged =
      request.userAnswers.get(AllWebsites).map(_.map(_.site)).getOrElse(Seq.empty) !=
        originalAnswers.schemeDetails.websites.map(_.websiteAddress)

    val contactDetailsChanged =
      request.userAnswers.get(BusinessContactDetailsPage).exists { contactDetails =>
        contactDetails.fullName != originalAnswers.schemeDetails.contactName ||
          contactDetails.telephoneNumber != originalAnswers.schemeDetails.businessTelephoneNumber ||
          contactDetails.emailAddress != originalAnswers.schemeDetails.businessEmailId
      }

    logger.info(s"countryBasedInChanged: $countryBasedInChanged")
    logger.info(s"taxReferenceChanged: $taxReferenceChanged")
    logger.info(s"clientBusinessNameChanged: $clientBusinessNameChanged")
    logger.info(s"clientBusinessAddressChanged: $clientBusinessAddressChanged")
    logger.info(s"tradingNamesChanged: $tradingNamesChanged")
    logger.info(s"previousRegistrationsChanged: $previousRegistrationsChanged")
    logger.info(s"fixedEstablishmentsChanged: $fixedEstablishmentsChanged")
    logger.info(s"amendedFixedEstablishmentsChanged: $amendedFixedEstablishmentsChanged")
    logger.info(s"previousRegistrationSchemesChanged: $previousRegistrationSchemesChanged")
    logger.info(s"websitesChanged: $websitesChanged")
    logger.info(s"contactDetailsChanged: $contactDetailsChanged")

    countryBasedInChanged ||
      taxReferenceChanged ||
      clientBusinessNameChanged ||
      clientBusinessAddressChanged ||
      tradingNamesChanged ||
      previousRegistrationsChanged ||
      fixedEstablishmentsChanged ||
      amendedFixedEstablishmentsChanged ||
      previousRegistrationSchemesChanged ||
      websitesChanged ||
      contactDetailsChanged
  }

  private def hasFixedEstablishmentDetailsChanged(
                                                   amendedDetails: EuDetails,
                                                   originalDetails: EtmpDisplayEuRegistrationDetails
                                                 ): Boolean = {

    val vatNumberWithoutCountryCode: Option[String] =
      amendedDetails.euVatNumber.map(_.stripPrefix(amendedDetails.euCountry.code))

    val originalRegistrationVatNumber: Option[String] =
      originalDetails.vatNumber

    amendedDetails.tradingNameAndBusinessAddress.map(_.tradingName.name).exists(_ != originalDetails.fixedEstablishmentTradingName) ||
      amendedDetails.tradingNameAndBusinessAddress.map(_.address).exists { address =>
        originalDetails.fixedEstablishmentAddressLine1 != address.line1 ||
          originalDetails.fixedEstablishmentAddressLine2 != address.line2 ||
          originalDetails.townOrCity != address.townOrCity ||
          originalDetails.regionOrState != address.stateOrRegion ||
          originalDetails.postcode != address.postCode
      } ||
      vatNumberWithoutCountryCode != originalRegistrationVatNumber ||
      amendedDetails.euTaxReference != originalDetails.taxIdentificationNumber
  }
}