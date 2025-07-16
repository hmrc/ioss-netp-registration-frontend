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
import forms.ClientVatNumberFormProvider
import logging.Logging
import models.core.Match
import models.responses.VatCustomerNotFound
import pages.{ClientVatNumberPage, UkVatNumberNotFoundPage, VatApiDownPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.ClientVatNumberView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientVatNumberController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: ClientVatNumberFormProvider,
                                           registrationConnector: RegistrationConnector,
                                           view: ClientVatNumberView,
                                           clock: Clock,
                                           coreRegistrationValidationService: CoreRegistrationValidationService
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[String] = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val preparedForm = request.userAnswers.get(ClientVatNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        ukVatNumber =>
          coreRegistrationValidationService.searchUkVrn(Vrn(ukVatNumber)).flatMap {

            case Some(activeMatch) if activeMatch.matchType.isActiveTrader && !activeMatch.traderId.isAnIntermediary =>
              Redirect(controllers.routes.ClientAlreadyRegisteredController.onPageLoad()).toFuture

            case Some(activeMatch) if activeMatch.matchType.isQuarantinedTrader && !activeMatch.traderId.isAnIntermediary =>
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

                  if (isExpired) {
                    logger.info(s"VAT number $ukVatNumber is expired (deregistration date: ${value.deregistrationDecisionDate})")
                    Redirect(controllers.routes.ExpiredVrnDateController.onPageLoad(waypoints).url).toFuture
                  } else {
                    for {
                      updatedAnswers <- Future.fromTry(request
                        .userAnswers
                        .copy(vatInfo = Some(value))
                        .set(ClientVatNumberPage, ukVatNumber))
                      _ <- cc.sessionRepository.set(updatedAnswers)
                    } yield Redirect(ClientVatNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
                  }
                case Left(VatCustomerNotFound) =>
                  Redirect(UkVatNumberNotFoundPage.route(waypoints).url).toFuture
                case Left(_) =>
                  Redirect(VatApiDownPage.route(waypoints).url).toFuture
              }
          }
      )
  }
}
