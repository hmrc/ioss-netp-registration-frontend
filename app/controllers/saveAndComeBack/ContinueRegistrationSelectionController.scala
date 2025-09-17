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

package controllers.saveAndComeBack

import connectors.{RegistrationConnector, SaveForLaterConnector}
import controllers.actions.AuthenticatedControllerComponents
import forms.ContinueRegistrationSelectionFormProvider
import logging.Logging
import models.{ClientBusinessName, SavedUserAnswers, UserAnswers}
import pages.{ClientBusinessNamePage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, ContinueRegistrationSelectionPage, JourneyRecoveryPage, QuestionPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveAndComeBackService
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationSelectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ContinueRegistrationSelectionController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         cc: AuthenticatedControllerComponents,
                                                         saveForLaterConnector: SaveForLaterConnector,
                                                         registrationConnector: RegistrationConnector,
                                                         formProvider: ContinueRegistrationSelectionFormProvider,
                                                         saveAndComeBackService: SaveAndComeBackService,
                                                         view: ContinueRegistrationSelectionView
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>

      request.userAnswers.getOrElse(UserAnswers(request.userId)).get(ContinueRegistrationSelectionPage) match {
          case Some(journeyId) =>
            saveAndComeBackService.retrieveSingleSavedUserAnswers(journeyId, waypoints)(request).flatMap {
              case Right(userAnswers) =>
                for {
                  _ <- cc.sessionRepository.set(userAnswers)
                } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())
              case Left(call) => Redirect(call).toFuture
            }

          case None =>
            saveForLaterConnector.getAllByIntermediary(request.intermediaryNumber.get).flatMap { //TODO - SCG- Don't use a .get

              case Right(seqSavedUserAnswers) if seqSavedUserAnswers.size == 1 => {
                val savedUserAnswers = seqSavedUserAnswers.head
                val updatedUserAnswers = UserAnswers(
                  request.userId,
                  savedUserAnswers.journeyId,
                  data = savedUserAnswers.data,
                  vatInfo = None,
                  lastUpdated = savedUserAnswers.lastUpdated) //TODO- SCG- Should this be userAnswers
                for {
                  _ <- cc.sessionRepository.set(updatedUserAnswers)
                } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())
              }

              case Right(seqSavedUserAnswers) =>
                saveAndComeBackService.fetchOutcomesFailFast(seqSavedUserAnswers).map { outcome =>

                  Ok(view(outcome, form, waypoints))
                }

              case Left(error) =>
                logger.warn(s"Failed to get the registration: $error")
                Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
            }
        }
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(UserAnswers(request.userId)).set(ContinueRegistrationSelectionPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(ContinueRegistrationSelectionPage.route(waypoints).url)
      )
  }
}