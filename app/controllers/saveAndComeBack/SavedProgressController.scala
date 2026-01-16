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
import controllers.actions.*
import formats.Format.saveForLaterDateFormatter
import logging.Logging
import models.{SaveForLaterRequest, SavedUserAnswers}
import pages.{JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.SavedProgressView

import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         saveForLaterConnector: SaveForLaterConnector,
                                         frontendAppConfig: FrontendAppConfig,
                                         view: SavedProgressView,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, continueUrl: RedirectUrl): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      val answersExpiry: String = request.userAnswers.lastUpdated.plus(frontendAppConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(clock.getZone).toLocalDate.format(saveForLaterDateFormatter)

      Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl.get(OnlyRelative).url)).flatMap { savedProgressAnswers =>
        val saveForLaterRequest: SaveForLaterRequest = SaveForLaterRequest(savedProgressAnswers, request.intermediaryNumber)
        (for {
          saveForLaterResult <- saveForLaterConnector.submit(saveForLaterRequest)
        } yield {
          saveForLaterResult
        }).flatMap {
          case Right(userAnswers: SavedUserAnswers) =>
            for {
              _ <- cc.sessionRepository.set(savedProgressAnswers)
            } yield {
              Ok(view(answersExpiry, continueUrl.get(OnlyRelative).url, frontendAppConfig.intermediaryYourAccountUrl))
            }
            
          case Left(error) =>
            logger.error(s"An unexpected error occurred when trying to submit saved user answers with error: ${error.body}")
            Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
        }
      }
  }
}
