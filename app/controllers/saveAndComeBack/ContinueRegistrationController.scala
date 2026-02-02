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

package controllers.saveAndComeBack

import controllers.actions.*
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import logging.Logging
import models.saveAndComeBack.{ContinueRegistration, TaxReferenceInformation}
import pages.{ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, SavedProgressPage, UkVatNumberNotFoundPage, VatApiDownPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.SaveAndComeBackService
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationView
import config.FrontendAppConfig
import models.responses.VatCustomerNotFound
import pages.previousRegistrations.PreviousIossNumberPage
import pages.vatEuDetails.EuVatNumberPage
import queries.previousRegistrations.AllPreviousRegistrationsRawQuery
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.domain.Vrn
import connectors.RegistrationConnector
import controllers.SetActiveTraderResult
import models.Index
import models.core.Match
import models.domain.VatCustomerInfo
import models.requests.DataRequest

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: ContinueRegistrationFormProvider,
                                                view: ContinueRegistrationView,
                                                frontendAppConfig: FrontendAppConfig,
                                                saveAndComeBackService: SaveAndComeBackService,
                                                coreRegistrationValidationService: CoreRegistrationValidationService,
                                                clock: Clock,
                                                registrationConnector: RegistrationConnector,

                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with SetActiveTraderResult {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      request.userAnswers.get(ClientVatNumberPage) match {
        case None => {
          val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)

          request.userAnswers.get(SavedProgressPage).map { _ =>
            Ok(view(taxReferenceInformation, form, waypoints)).toFuture
          }.getOrElse {
            val exception = new IllegalStateException("Must have a saved page url to return to the saved journey")
            logger.error(exception.getMessage, exception)
            throw exception
          }
        }

        case Some(clientVatNumber) =>

          saveAndComeBackService.getVatTaxInfo(clientVatNumber, waypoints).map { vatCustomerInfo =>
            val updatedAnswers = request.userAnswers.copy(vatInfo = Some(vatCustomerInfo))
            cc.sessionRepository.set(updatedAnswers)

            val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(updatedAnswers)

            request.userAnswers.get(SavedProgressPage).map { _ =>
              Ok(view(taxReferenceInformation, form, waypoints))
            }.getOrElse {
              val exception = new IllegalStateException("Must have a saved page url to return to the saved journey")
              logger.error(exception.getMessage, exception)
              throw exception
            }
          }
      }

  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)
      val dashboardUrl = frontendAppConfig.intermediaryYourAccountUrl
      val alreadyRegisteredRedirect = controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url
      val vatExpiredRedirect = controllers.routes.ExpiredVrnDateController.onPageLoad(waypoints).url
      
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(taxReferenceInformation, formWithErrors, waypoints)).toFuture,

        value1 =>
          (value1, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) =>
              request.userAnswers.get(ClientVatNumberPage) match {
                case Some(vatNumber) =>
                  coreRegistrationValidationService.searchUkVrn(Vrn(vatNumber)).flatMap {
                    case Some(activeMatch: Match) => 
                      if(checkIfVatRegistered(activeMatch)) then deleteAndRedirect(request,taxReferenceInformation,alreadyRegisteredRedirect)
                      else if(checkIfQuarantined(activeMatch)) then deleteAndRedirect(request, taxReferenceInformation, controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(activeMatch.memberState, activeMatch.getEffectiveDate).url)
                      else logger.info("VAT Number is still valid")
                    case _ =>
                      if(checkVatExpired(request.userAnswers.vatInfo)) then deleteAndRedirect(request, taxReferenceInformation, vatExpiredRedirect)
                      else logger.info("VAT has not expired")
                  }
              }
              
            case (ContinueRegistration.Delete, _) =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveAndComeBackService.deleteSavedUserAnswers(taxReferenceInformation.journeyId)
              } yield Redirect(dashboardUrl)

            case _ =>
              val exception = new IllegalStateException("Illegal value submitted and/or must have a saved page url to return to the saved journey")
              logger.error(exception.getMessage, exception)
              throw exception
          }
      )

  }
  
  
  private def checkIfVatRegistered(activeMatch: Match): Boolean = {
    activeMatch.isActiveTrader(clock)
  }
  
  private def checkIfQuarantined(activeMActh: Match): Boolean = {
    activeMatch.isQuarantinedTrader(clock)
  }
  
  private def checkVatExpired(vatCustomerInfo: Option[VatCustomerInfo]): Boolean = {
      case Some(vatCustomerInfo) =>
        val today = LocalDate.now(clock)
        val isExpired = vatCustomerInfo.deregistrationDecisionDate.exists(!_.isAfter(today))
        isExpired
      case None => logger.info("No VAT number found for the user, cannot check if it is expired")
  }
  
  
  private def deleteAndRedirect(request: DataRequest[AnyContent], taxReferenceInformation: TaxReferenceInformation, redirectUrl: String)
                               (implicit ec: ExecutionContext): Future[Result] = cc.identifyAndGetData().async {
    for {
      _ <- cc.sessionRepository.clear(request.userId)
      _ <- saveAndComeBackService.deleteSavedUserAnswers(taxReferenceInformation.journeyId)
    } yield Redirect(redirectUrl)
  }

}
