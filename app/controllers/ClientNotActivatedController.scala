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

package controllers

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.*
import logging.Logging
import models.{BusinessContactDetails, UserAnswers}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import pages.{BusinessBasedInUKPage, BusinessContactDetailsPage, ClientHasVatNumberPage, ClientVatNumberPage, JourneyRecoveryPage, Waypoints}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.CheckVatDetailsViewModel
import viewmodels.checkAnswers.{BusinessBasedInUKSummary, BusinessContactDetailsSummary, ClientBusinessAddressSummary, ClientBusinessNameSummary, ClientCountryBasedSummary, ClientHasUtrNumberSummary, ClientHasVatNumberSummary, ClientTaxReferenceSummary, ClientUtrNumberSummary, ClientVatNumberSummary, ClientsNinoNumberSummary}
import views.html.ClientNotActivatedView
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.govuk.summarylist.*
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}

import scala.concurrent.ExecutionContext

class ClientNotActivatedController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       val controllerComponents: MessagesControllerComponents,
                                       registrationConnector: RegistrationConnector,
                                       frontendAppConfig: FrontendAppConfig,
                                       view: ClientNotActivatedView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetClientCompanyName with Logging {
  
  def onPageLoad(waypoints: Waypoints, journeyId: String): Action[AnyContent] = (cc.actionBuilder andThen cc.identify).async {
    implicit request =>
      registrationConnector.getPendingRegistrationsByIntermediaryNumber(request.intermediaryNumber).map {
        case Right(savedPendingRegistrations) =>
          savedPendingRegistrations.find(_.journeyId == journeyId) match {
            case Some(registration) =>
              val emailAddress = registration.userAnswers.get(BusinessContactDetailsPage).get.emailAddress

              val isBasedInUk = registration.userAnswers.get(BusinessBasedInUKPage).getOrElse(false)
              val hasVatNumber = registration.userAnswers.get(ClientHasVatNumberPage).getOrElse(false)
              val ukVatNumber = registration.userAnswers.get(ClientVatNumberPage).getOrElse("")

              val registrationSummaryList = buildRegistrationSummaryList(waypoints, registration.userAnswers)
              val clientDetailsSummaryList = buildClientDetailsSummaryList(waypoints, registration.userAnswers)

              val clientCodeEntryUrl = s"${frontendAppConfig.clientCodeEntryUrl}/${registration.uniqueUrlCode}"
              val activationExpiryDate = registration.activationExpiryDate
              val clientCompanyName = registration.userAnswers.vatInfo.get.organisationName
                .getOrElse(registration.userAnswers.vatInfo.get.individualName.getOrElse(""))

              if (isBasedInUk && hasVatNumber) {
                registration.userAnswers.vatInfo match {

                  case Some(vatCustomerInfo) =>
                    val viewModel = CheckVatDetailsViewModel(ukVatNumber, vatCustomerInfo)

                    Ok(view(waypoints, Some(viewModel), registrationSummaryList, clientDetailsSummaryList, clientCompanyName, isBasedInUk, hasVatNumber, emailAddress, clientCodeEntryUrl, activationExpiryDate))

                  case None =>
                    Redirect(JourneyRecoveryPage.route(waypoints).url)
                }
              } else {
                Ok(view(waypoints, None, registrationSummaryList, clientDetailsSummaryList, clientCompanyName, isBasedInUk, hasVatNumber, emailAddress, clientCodeEntryUrl, activationExpiryDate))
              }
            case None => Redirect(JourneyRecoveryPage.route(waypoints).url)
          }

        case Left(errors) =>
          val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given intermediary number: $errors."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  private def buildRegistrationSummaryList(
                                waypoints: Waypoints,
                                userAnswers: UserAnswers)
                              (implicit messages: Messages): SummaryList = {

    val hasUkVatNumber = userAnswers.get(ClientHasVatNumberPage).contains(true)
    val isUKBased = userAnswers.get(BusinessBasedInUKPage).contains(true)

    val rows = Seq(
      BusinessBasedInUKSummary.rowWithoutAction(waypoints, userAnswers),
      ClientHasVatNumberSummary.rowWithoutAction(waypoints, userAnswers),
      ClientVatNumberSummary.rowWithoutAction(waypoints, userAnswers),
      ClientCountryBasedSummary.rowWithoutAction(waypoints, userAnswers),
      ClientTaxReferenceSummary.rowWithoutAction(waypoints, userAnswers),
      ClientBusinessNameSummary.rowWithoutAction(waypoints, userAnswers),
      if (isUKBased && !hasUkVatNumber) ClientHasUtrNumberSummary.rowWithoutAction(waypoints, userAnswers) else None,
      ClientUtrNumberSummary.rowWithoutAction(waypoints, userAnswers),
      ClientsNinoNumberSummary.rowWithoutAction(waypoints, userAnswers),
      ClientBusinessAddressSummary.rowWithoutAction(waypoints, userAnswers)
    ).flatten

    SummaryListViewModel(rows = rows)
  }

  private def buildClientDetailsSummaryList(
                                    waypoints: Waypoints,
                                    userAnswers: UserAnswers)
                                    (implicit messages: Messages): SummaryList =

    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.rowWithoutAction(userAnswers, waypoints)

    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRowWithoutAction(waypoints, userAnswers)
    val previouslyRegisteredSummaryRow = PreviouslyRegisteredSummary.rowWithoutAction(userAnswers, waypoints)
    val previousRegistrationSummaryRow = PreviousRegistrationSummary.checkAnswersRowWithoutAction(userAnswers, Seq.empty, waypoints)
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.rowWithoutAction(waypoints, userAnswers)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRowWithoutAction(waypoints, userAnswers)
    val contactDetailsFullNameRow = BusinessContactDetailsSummary.rowFullNameWithoutAction(waypoints, userAnswers)
    val contactDetailsTelephoneNumberRow = BusinessContactDetailsSummary.rowTelephoneNumberWithoutAction(waypoints, userAnswers)
    val contactDetailsEmailAddressRow = BusinessContactDetailsSummary.rowEmailAddressWithoutAction(waypoints, userAnswers)

    val list = SummaryListViewModel(
      rows = Seq(
        maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
          if (tradingNameSummaryRow.nonEmpty) {
            hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            hasTradingNameSummaryRow
          }
        },
        tradingNameSummaryRow,
        previouslyRegisteredSummaryRow.map { sr =>
          if (previousRegistrationSummaryRow.isDefined) {
            sr.withCssClass("govuk-summary-list__row--no-border")
          } else {
            sr
          }
        },
        previousRegistrationSummaryRow,
        maybeHasFixedEstablishmentSummaryRow.map { sr =>
          if (euDetailsSummaryRow.nonEmpty) {
            sr.withCssClass("govuk-summary-list__row--no-border")
          } else {
            sr.withCssClass("govuk-summary-list")
          }
        },
        euDetailsSummaryRow,
        contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        contactDetailsEmailAddressRow,
      ).flatten
    )

    list

}

