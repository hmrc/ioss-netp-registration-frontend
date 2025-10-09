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

import controllers.GetClientCompanyName
import controllers.actions.AuthenticatedControllerComponents
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import logging.Logging
import pages.{CheckYourAnswersPage, ClientVatNumberPage, EmptyWaypoints, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ChangeRegistrationView
import utils.FutureSyntax.FutureOps
import viewmodels.WebsiteSummary
import viewmodels.checkAnswers.vatEuDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.previousRegistrations.{PreviousRegistrationSummary, PreviouslyRegisteredSummary}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: ChangeRegistrationView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientCompanyName {

  def onPageLoad(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      val waypoints = EmptyWaypoints
      val userAnswers = request.userAnswers
      val thisPage = CheckYourAnswersPage

      println("\n\nrequest.intermediaryNumber:")
      println(request.intermediaryNumber)

      getClientCompanyName(waypoints) { companyName =>
        val registrationDetailsList = SummaryListViewModel(
          rows = Seq(
            //  Based in UK
            BusinessBasedInUKSummary.row(waypoints, request.userAnswers, CheckYourAnswersPage),
            //Has UK VAT registration number
            ClientHasVatNumberSummary.row(waypoints, request.userAnswers, CheckYourAnswersPage),
            ClientVatNumberSummary.row(waypoints, request.userAnswers, CheckYourAnswersPage),
            VatRegistrationDetailsSummary.rowBusinessAddress(waypoints, request.userAnswers, CheckYourAnswersPage)
            //Trading name
            //Has Unique Taxpayer Reference (UTR) number
            //UK VAT registration number
            //Principal place of business address
            //Trading name
          ).flatten
        )
        val importOneStopShopDetailsList = SummaryListViewModel(
          rows = Seq(
            //  Based in UK
            HasTradingNameSummary.row(request.userAnswers, waypoints, CheckYourAnswersPage),
            TradingNameSummary.checkAnswersRow(waypoints, userAnswers, thisPage),
        PreviouslyRegisteredSummary.row(userAnswers, waypoints, thisPage),
        PreviousRegistrationSummary.checkAnswersRow(userAnswers, Seq.empty, waypoints, thisPage),
        HasFixedEstablishmentSummary.row(waypoints, userAnswers, thisPage),
        EuDetailsSummary.checkAnswersRow(waypoints, userAnswers, thisPage),
        WebsiteSummary.checkAnswersRow(waypoints, userAnswers, thisPage), // Trading websites (Not Currently Mapped to UA)
        BusinessContactDetailsSummary.rowFullName(waypoints, userAnswers, thisPage),
        BusinessContactDetailsSummary.rowTelephoneNumber(waypoints, userAnswers, thisPage),
        BusinessContactDetailsSummary.rowEmailAddress(waypoints, userAnswers, thisPage)
        ).flatten
        )

        Ok(view(waypoints, companyName, iossNumber, registrationDetailsList, importOneStopShopDetailsList)).toFuture

      }


  }

  def onSubmit(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.identifyAndGetData {
    Ok(Json.toJson("done"))
  }
}

/**
 * ClientHasVatNumberSummary.row(waypoints, userAnswers, thisPage),
 * ClientVatNumberSummary.row(waypoints, userAnswers, thisPage),
 * ClientBusinessNameSummary.row(waypoints, userAnswers, thisPage),
 * ClientHasUtrNumberSummary.row(waypoints, userAnswers, thisPage),
 * ClientUtrNumberSummary.row(waypoints, userAnswers, thisPage),
 * ClientsNinoNumberSummary.row(waypoints, userAnswers, thisPage),
 * ClientCountryBasedSummary.row(waypoints, userAnswers, thisPage),
 * ClientTaxReferenceSummary.row(waypoints, userAnswers, thisPage),
 * ClientBusinessAddressSummary.row(waypoints, userAnswers, thisPage),
 */
