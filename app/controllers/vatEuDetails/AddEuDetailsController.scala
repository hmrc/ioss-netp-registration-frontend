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

package controllers.vatEuDetails

import controllers.actions.*
import forms.vatEuDetails.AddEuDetailsFormProvider
import models.Country
import models.vatEuDetails.EuDetails
import pages.vatEuDetails.AddEuDetailsPage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.euDetails.DeriveNumberOfEuRegistrations
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.EuDetailsCompletionChecks.*
import utils.ItemsHelper.getDerivedItems
import utils.AmendWaypoints.AmendWaypointsOps
import viewmodels.checkAnswers.vatEuDetails.EuDetailsSummary
import views.html.vatEuDetails.AddEuDetailsView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEuDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: AddEuDetailsFormProvider,
                                         view: AddEuDetailsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val euCountriesSize: Int = Country.euCountries.size
  private val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.identifyAndGetData(waypoints.inAmend, checkAmendAccess = Some(AddEuDetailsPage())).async {
    implicit request =>

      getDerivedItems(waypoints, DeriveNumberOfEuRegistrations) { numberOfEuRegistrations =>

        val canAddEuDetails: Boolean = numberOfEuRegistrations < euCountriesSize
        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, request.userAnswers, AddEuDetailsPage())
        
        withCompleteDataAsync[EuDetails](
          data = getAllIncompleteEuDetails _,
          onFailure = (incomplete: Seq[EuDetails]) => {
            Ok(view(form, waypoints, euDetailsSummaryList, canAddEuDetails, incomplete)).toFuture
          }) {
          Ok(view(form, waypoints, euDetailsSummaryList, canAddEuDetails)).toFuture
        }

      }
  }

  def onSubmit(waypoints: Waypoints, incompletePromptShown: Boolean): Action[AnyContent] = cc.identifyAndGetData(waypoints.inAmend).async {
    implicit request =>

      withCompleteDataAsync[EuDetails](
        data = getAllIncompleteEuDetails _,
        onFailure = (incomplete: Seq[EuDetails]) => {
          if (incompletePromptShown) {
            incompleteEuDetailsRedirect(waypoints).map { redirectIncompletePage =>
              redirectIncompletePage.toFuture
            }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
          } else {
            Redirect(AddEuDetailsPage().route(waypoints).url).toFuture
          }
        }) {
        
        getDerivedItems(waypoints, DeriveNumberOfEuRegistrations) { numberOfEuRegistrations =>

          val canAddEuDetails: Boolean = numberOfEuRegistrations < euCountriesSize
          val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, request.userAnswers, AddEuDetailsPage())

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, euDetailsSummaryList, canAddEuDetails)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddEuDetailsPage(), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddEuDetailsPage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        }
      }
  }
}
