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

package controllers

import controllers.actions.*
import forms.ClientsNinoNumberFormProvider
import logging.Logging
import models.{SavedUserAnswers, UserAnswers}
import models.core.Match
import models.requests.DataRequest
import models.saveAndComeBack.{MultipleRegistrations, SingleRegistration, TaxReferenceInformation}
import pages.{ClientsNinoNumberPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveAndComeBackService
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.ClientsNinoNumberView

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientsNinoNumberController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: ClientsNinoNumberFormProvider,
                                             view: ClientsNinoNumberView,
                                             coreRegistrationValidationService: CoreRegistrationValidationService,
                                             clock: Clock,
                                             saveAndComeBackService: SaveAndComeBackService
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging with GetCountry with SetActiveTraderResult {

  val form: Form[String] = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(inAmend = waypoints.inAmend, checkAmendAccess = Some(ClientsNinoNumberPage)) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ClientsNinoNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(inAmend = waypoints.inAmend).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          coreRegistrationValidationService.searchTraderId(value).flatMap {

            case Some(activeMatch) if activeMatch.isActiveTrader(clock) =>
              setActiveTraderResultAndRedirect(
                activeMatch = activeMatch,
                sessionRepository = cc.sessionRepository,
                redirect = controllers.routes.ClientAlreadyRegisteredController.onPageLoad()
              )
              
            case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
              Redirect(
                controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                  activeMatch.memberState,
                  activeMatch.getEffectiveDate)
              ).toFuture

            case _ =>
              saveAndComeBackService.getSavedContinueRegistrationJourneys(request.userAnswers, request.intermediaryNumber).flatMap {

                case SingleRegistration(singleJourneyId) =>
                  continueJourney(Some(singleJourneyId), ninoNumber = value, request, waypoints)

                case MultipleRegistrations(multipleRegistrations) =>

                  val taxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)

                  val maybeJourneyId = findMatchingJourneyId(taxReferenceInformation, multipleRegistrations)

                  continueJourney(maybeJourneyId, ninoNumber = value, request, waypoints)

                case _ =>
                  continueJourney(maybeJourneyId = None, ninoNumber = value, request, waypoints)
              }
          }
      )
  }

  private def handleRedirect(
                                   journeyId: String,
                                   ninoNumber: String,
                                   waypoints: Waypoints,
                                   updatedAnswers: UserAnswers
                                   )(implicit request: DataRequest[_]): Future[Result] =

    saveAndComeBackService.retrieveSingleSavedUserAnswer(journeyId, waypoints).flatMap { savedUserAnswers =>

      val ninoFromDatabase: Option[String] = (savedUserAnswers.data \ "clientsNinoNumber").asOpt[String]

      if (ninoFromDatabase.contains(ninoNumber)) {
        Redirect(controllers.routes.ClientRegistrationAlreadyPendingController.onPageLoad(waypoints).url).toFuture
      } else {
        Redirect(ClientsNinoNumberPage.navigate(waypoints, updatedAnswers, updatedAnswers).route).toFuture
      }
    }

  private def continueJourney(
                            maybeJourneyId: Option[String],
                            ninoNumber: String,
                            request: DataRequest[_],
                            waypoints: Waypoints
                          ): Future[Result] =

    val answersWithJourneyId = maybeJourneyId.fold(request.userAnswers)(id => request.userAnswers.copy(journeyId = id))

    for {
      updatedAnswers <- Future.fromTry(answersWithJourneyId.set(ClientsNinoNumberPage, ninoNumber))
      _              <- cc.sessionRepository.set(updatedAnswers)
      result         <- maybeJourneyId match {

        case Some(journeyId) =>
          handleRedirect(journeyId, ninoNumber, waypoints, updatedAnswers)(request)

        case None =>
          Redirect(ClientsNinoNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route).toFuture
      }
    } yield result

  private def findMatchingJourneyId(
                                     taxReferenceInformation: TaxReferenceInformation,
                                     multipleRegistrations: Seq[SavedUserAnswers]
                                   ): Option[String] =
    multipleRegistrations
      .find(_.journeyId == taxReferenceInformation.journeyId)
      .map(_.journeyId)

}
