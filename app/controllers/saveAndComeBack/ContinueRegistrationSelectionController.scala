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

import connectors.SaveForLaterConnector
import controllers.actions.AuthenticatedControllerComponents
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import controllers.saveAndComeBack.ContinueRegistrationController
import logging.Logging
import models.UserAnswers
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationSelectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationSelectionController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         cc: AuthenticatedControllerComponents,
                                                         saveForLaterConnector: SaveForLaterConnector,
                                                         view: ContinueRegistrationSelectionView
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>
      println(s"\n\n\n${request.intermediaryNumber.get}")
      saveForLaterConnector.getAllByIntermediary(request.intermediaryNumber.get).flatMap { //TODO - SCG- Don't use a .get

        case Right(seqSavedUserAnswers) if seqSavedUserAnswers.size == 1 => {
          println(s"\n\n seq $seqSavedUserAnswers")
          val savedUserAnswers = seqSavedUserAnswers.head
          val updatedUserAnswers = UserAnswers(request.userId, savedUserAnswers.journeyId, data = savedUserAnswers.data, vatInfo = None, lastUpdated = savedUserAnswers.lastUpdated) //TODO- SCG- Should this be userAnswers
          for {
            _ <- cc.sessionRepository.set(updatedUserAnswers)
          } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())
        }

        case Right(seqSavedUserAnswers) =>
          // view(form = ???, waypoints)
          println("\n\n\nSCG Not yet implemented 1")
          logger.warn("Not Yet implemented 1")
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture

        case Left(error) =>
          println("\n\n\nSCG Not yet implemented 3")
          logger.warn(s"Failed to get the registration: $error")
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
      }
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>
      ???
  }
}
