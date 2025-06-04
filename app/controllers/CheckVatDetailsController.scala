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

import connectors.RegistrationConnector
import controllers.actions.*
import forms.CheckVatDetailsFormProvider
import models.UserAnswers

import javax.inject.Inject
import models.checkVatDetails.CheckVatDetails
import pages.*
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.CheckVatDetailsViewModel
import viewmodels.checkAnswers.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.{CheckVatDetailsView, ConfirmClientVatDetailsView}

import scala.concurrent.{ExecutionContext, Future}

class CheckVatDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: CheckVatDetailsFormProvider,
                                         registrationConnector: RegistrationConnector,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: CheckVatDetailsView,
                                         nonUkVatNumberView: ConfirmClientVatDetailsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetClientCompanyName {

  val form: Form[CheckVatDetails] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      
      val userAnswers = request.userAnswers
      val ukVatNumber = userAnswers.get(ClientVatNumberPage)
      val sourcePage = CheckVatDetailsPage()

      ukVatNumber match {
        case Some(ukVatNumber) =>
          registrationConnector.getVatCustomerInfo(ukVatNumber).flatMap {
            case Right(vatInfo) =>
              val preparedForm = userAnswers.get(CheckVatDetailsPage()) match {
                case None => form
                case Some(value) => form.fill(value)
              }

              val companyName = vatInfo.organisationName.getOrElse("")

              val summaryList = buildSummaryList(waypoints, userAnswers, sourcePage)

              val viewModel = CheckVatDetailsViewModel(ukVatNumber, vatInfo)

              Ok(view(preparedForm, waypoints, viewModel, summaryList, companyName)).toFuture

            case _ =>
              Redirect(VatApiDownPage.route(waypoints).url).toFuture
          }

        case None =>
          getClientCompanyName(waypoints) { companyName =>
            val summaryList = buildSummaryList(waypoints, userAnswers, sourcePage)
            Ok(nonUkVatNumberView(waypoints, summaryList, companyName.name)).toFuture
          }

      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val ukVatNumber = request.userAnswers.get(ClientVatNumberPage)


      ukVatNumber match {
        case Some(ukVatNumber) =>
          registrationConnector.getVatCustomerInfo(ukVatNumber).flatMap {
            case Right(vatInfo) =>
              form.bindFromRequest().fold(
                formWithErrors => {
                  val companyName = vatInfo.organisationName.getOrElse("")

                  val viewModel = CheckVatDetailsViewModel(ukVatNumber, vatInfo)
                  val sourcePage = CheckVatDetailsPage()

                  val summaryList = buildSummaryList(waypoints, request.userAnswers, sourcePage)

                  BadRequest(view(formWithErrors, waypoints, viewModel, summaryList, companyName)).toFuture
                },

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckVatDetailsPage(), value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(CheckVatDetailsPage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
              )

            case _ =>
              Redirect(VatApiDownPage.route(waypoints)).toFuture
          }
        case None =>
          Redirect(CheckVatDetailsPage().navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
      }


  }

  private def buildSummaryList(
                                waypoints: Waypoints,
                                userAnswers: UserAnswers,
                                sourcePage: CheckAnswersPage)
                              (implicit messages: Messages): SummaryList = {

    val hasUkVatNumber = userAnswers.get(ClientHasVatNumberPage).contains(true)
    val isUKBased = userAnswers.get(BusinessBasedInUKPage).contains(true)

    val rows = Seq(
      BusinessBasedInUKSummary.row(waypoints, userAnswers, sourcePage),
      if (isUKBased) ClientHasVatNumberSummary.row(waypoints, userAnswers, sourcePage) else None,
      ClientVatNumberSummary.row(waypoints, userAnswers, sourcePage),
      ClientBusinessNameSummary.row(waypoints, userAnswers, sourcePage),
      if (isUKBased && !hasUkVatNumber) ClientHasUtrNumberSummary.row(waypoints, userAnswers, sourcePage) else None,
      ClientUtrNumberSummary.row(waypoints, userAnswers, sourcePage),
      ClientsNinoNumberSummary.row(waypoints, userAnswers, sourcePage),
      ClientCountryBasedSummary.row(waypoints, userAnswers, sourcePage),
      ClientTaxReferenceSummary.row(waypoints, userAnswers, sourcePage),
      ClientBusinessAddressSummary.row(waypoints, userAnswers, sourcePage)
    ).flatten

    SummaryListViewModel(rows = rows)
  }

}
