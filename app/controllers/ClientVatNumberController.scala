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

import connectors.RegistrationConnector
import controllers.actions.*
import forms.ClientVatNumberFormProvider
import logging.Logging
import models.{SavedUserAnswers, UserAnswers}
import models.core.Match
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import models.responses.VatCustomerNotFound
import models.saveAndComeBack.{MultipleRegistrations, SingleRegistration, TaxReferenceInformation}
import pages.{ClientVatNumberPage, UkVatNumberNotFoundPage, VatApiDownPage, Waypoints}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveAndComeBackService
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.ClientVatNumberView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientVatNumberController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: ClientVatNumberFormProvider,
                                           registrationConnector: RegistrationConnector,
                                           view: ClientVatNumberView,
                                           clock: Clock,
                                           coreRegistrationValidationService: CoreRegistrationValidationService,
                                           saveAndComeBackService: SaveAndComeBackService
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with SetActiveTraderResult {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData(inAmend = waypoints.inAmend, checkAmendAccess = Some(ClientVatNumberPage)) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ClientVatNumberPage) match {
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

        ukVatNumber =>
          coreRegistrationValidationService.searchUkVrn(Vrn(ukVatNumber)).flatMap {

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
              registrationConnector.getVatCustomerInfo(ukVatNumber).flatMap {
                case Right(value) =>
                  val today = LocalDate.now(clock)
                  val isExpired = value.deregistrationDecisionDate.exists(!_.isAfter(today))
                  
                  def handleSelection(maybeJourneyId: Option[String]): Future[Result] = {
                    
                    handleExpiry(isExpired, ukVatNumber, value.deregistrationDecisionDate, waypoints)
                      .getOrElse( {
                        continueJourney(maybeJourneyId, request, value, ukVatNumber, waypoints)
                      })
                  }
                  
                  saveAndComeBackService.getSavedContinueRegistrationJourneys(request.userAnswers, request.intermediaryNumber).flatMap {

                    case SingleRegistration(singleJourneyId) =>
                      handleSelection(Some(singleJourneyId))
                      
                    case MultipleRegistrations(multipleRegistrations) =>
                      val taxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)
                      
                      val matchingJourneyId = findMatchingJourneyId(taxReferenceInformation, multipleRegistrations)
                      
                      handleSelection(matchingJourneyId)
                      
                    case _ =>
                      handleSelection(None)
                  }

                case Left(VatCustomerNotFound) =>
                  Redirect(UkVatNumberNotFoundPage.route(waypoints).url).toFuture
                case Left(_) =>
                  Redirect(VatApiDownPage.route(waypoints).url).toFuture
              }
          }
      )
  }

  private def handleExpiry(
                          isExpired: Boolean,
                          ukVatNumber: String,
                          deregistrationDecisionDate: Option[LocalDate],
                          waypoints: Waypoints
                          ): Option[Future[Result]] =
    if (isExpired) {
      logger.info(s"VAT number $ukVatNumber is expired (deregistration date: $deregistrationDecisionDate)")
      Some(Redirect(controllers.routes.ExpiredVrnDateController.onPageLoad(waypoints).url).toFuture)
    } else {
      None
    }

  private def handleRedirect(
                             journeyId: String,
                             ukVatNumber: String,
                             waypoints: Waypoints,
                             updatedUserAnswers: UserAnswers
                            )(implicit request: DataRequest[_]): Future[Result] =
    
    saveAndComeBackService.retrieveSingleSavedUserAnswer(journeyId, waypoints)
      .map { savedUserAnswers =>
        
        val vatNumberFromDatabase = (savedUserAnswers.data \ "clientVatNumber").asOpt[String]
        
        if (vatNumberFromDatabase.contains(ukVatNumber)) {
          Redirect(controllers.routes.ClientRegistrationAlreadyPendingController.onPageLoad(waypoints).url)
        } else {
          Redirect(ClientVatNumberPage.navigate(waypoints, updatedUserAnswers, updatedUserAnswers).route)
        }
      }

  private def continueJourney(
                          maybeJourneyId: Option[String],
                          request: DataRequest[_],
                          vatInfo: VatCustomerInfo,
                          ukVatNumber: String,
                          waypoints: Waypoints
                          ): Future[Result] =

    val baseAnswers = request.userAnswers.copy(vatInfo = Some(vatInfo))
    
    val answersWithJourney = maybeJourneyId.fold(baseAnswers)(id => baseAnswers.copy(journeyId = id))
    
    for {
      updatedAnswers <- Future.fromTry(answersWithJourney.set(ClientVatNumberPage, ukVatNumber))
      _ <- cc.sessionRepository.set(updatedAnswers)
      result <- maybeJourneyId match {
        case Some(journeyId) => handleRedirect(journeyId, ukVatNumber, waypoints, updatedAnswers)(request)
        case None =>
          Redirect(ClientVatNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route).toFuture
      }
    } yield result
    
  private def findMatchingJourneyId(
                                    taxReferenceInformation: TaxReferenceInformation,
                                    multipleRegistrations: Seq[SavedUserAnswers]
                                   ): Option[String] = {
    multipleRegistrations
      .find(_.journeyId == taxReferenceInformation.journeyId)
      .map(_.journeyId)
  }
}
