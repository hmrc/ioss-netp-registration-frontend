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

import config.FrontendAppConfig
import connectors.SaveForLaterConnector
import controllers.actions.AuthenticatedControllerComponents
import forms.saveAndComeBack.ContinueRegistrationSelectionFormProvider
import logging.Logging
import models.UserAnswers
import models.saveAndComeBack.{ContinueRegistrationList, MultipleRegistrations, NoRegistrations, SingleRegistration}
import pages.{ContinueRegistrationSelectionPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveAndComeBackService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationSelectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ContinueRegistrationSelectionController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         cc: AuthenticatedControllerComponents,
                                                         formProvider: ContinueRegistrationSelectionFormProvider,
                                                         saveAndComeBackService: SaveAndComeBackService,
                                                         frontendAppConfig: FrontendAppConfig,
                                                         view: ContinueRegistrationSelectionView,
                                                         saveForLaterConnector: SaveForLaterConnector
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData().async {
    implicit request =>

      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

      val dashboardUrl = frontendAppConfig.intermediaryYourAccountUrl

      saveAndComeBackService.getSavedContinueRegistrationJourneys(userAnswers, request.intermediaryNumber.get).flatMap {
        case SingleRegistration(singleJourneyId) =>

          saveAndComeBackService.retrieveSingleSavedUserAnswers(singleJourneyId, waypoints).flatMap { enrichedUserAnswers =>

            for {
              _ <- cc.sessionRepository.set(enrichedUserAnswers)
            } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())

          }

        case MultipleRegistrations(multipleRegistrations) =>
          val form: Form[String] = formProvider(multipleRegistrations)

          saveAndComeBackService.createTaxReferenceInfoForSavedUserAnswers(multipleRegistrations).flatMap { seqTaxReferenceInfo =>
            for {
              updatedAnswers <- Future.fromTry(userAnswers.set(ContinueRegistrationList, seqTaxReferenceInfo))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Ok(view(seqTaxReferenceInfo, form, waypoints))
          }

        case NoRegistrations =>
          Redirect(dashboardUrl).toFuture
      }
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      saveForLaterConnector.getAllByIntermediary(request.intermediaryNumber).flatMap {
        case Right(savedUserAnswers) =>
          val form = formProvider(savedUserAnswers)
          
          form.bindFromRequest().fold(
            formWithErrors =>
              request.userAnswers.get(ContinueRegistrationList) match
                case Some(seqTaxRefInfo) =>
                  BadRequest(view(seqTaxRefInfo, formWithErrors, waypoints)).toFuture
                case None =>
                  val message: String = s"Received an unexpected error as no registration list found"
                  val exception: IllegalStateException = new IllegalStateException(message)
                  logger.error(exception.getMessage, exception)
                  throw exception,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ContinueRegistrationSelectionPage, value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield {
                Redirect(ContinueRegistrationSelectionPage.route(waypoints).url)
              }
          )
          
        case Left(error) =>
          val message: String = s"Received an unexpected error when trying to retrieve uncompleted " +
            s"registrations for the intermediary ID: ${request.intermediaryNumber}. \nWith Errors: $error"
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
      
  }
}
