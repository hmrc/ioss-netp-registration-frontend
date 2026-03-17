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
import controllers.SetActiveTraderResult
import controllers.actions.*
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import logging.Logging
import models.etmp.EtmpIdType
import models.etmp.EtmpIdType.{FTR, NINO, UTR, VRN}
import models.requests.DataRequest
import models.saveAndComeBack.ContinueRegistration
import pages.{SavedProgressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Reads
import play.api.mvc.*
import queries.PreviousUnfinishedRegistration
import services.SaveAndComeBackService
import services.core.CoreSavedAnswersRevalidationService
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.RegistrationAlreadySavedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationAlreadySavedController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    cc: AuthenticatedControllerComponents,
                                                    formProvider: ContinueRegistrationFormProvider,
                                                    view: RegistrationAlreadySavedView,
                                                    saveAndComeBackService: SaveAndComeBackService,
                                                    coreSavedAnswersRevalidationService: CoreSavedAnswersRevalidationService
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with SetActiveTraderResult {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>
      request.userAnswers.get(PreviousUnfinishedRegistration) match
        case None =>
          val exception = new IllegalStateException("Must have previous unfinished registration journey")
          logger.error(exception.getMessage, exception)
          throw exception
        case Some(previousUserAnswers) =>
          val (companyName: String, taxReference: String, _) = saveAndComeBackService.retrieveTaxRef(previousUserAnswers)
          previousUserAnswers.get(SavedProgressPage).map { _ =>
            Ok(view(form, waypoints, companyName, taxReference)).toFuture
          }.getOrElse {
            val exception = new IllegalStateException("Must have a saved page url to return to the saved journey")
            logger.error(exception.getMessage, exception)
            throw exception
          }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>
      request.userAnswers.get(PreviousUnfinishedRegistration) match {
        case Some(previousUserAnswers) =>
          val (companyName: String, taxReference: String, etmpIdType: EtmpIdType) = saveAndComeBackService.retrieveTaxRef(previousUserAnswers)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(form, waypoints, companyName, taxReference)).toFuture,

            value1 =>
              (value1, previousUserAnswers.get(SavedProgressPage)) match {
                case (ContinueRegistration.Continue, Some(url)) =>
                  coreSavedAnswersRevalidationService.checkAndValidateSavedUserAnswers(waypoints).flatMap {
                    case Some(redirectResult) =>
                      deleteAndRedirect(previousUserAnswers.journeyId, redirectResult)

                    case None =>
                      Redirect(Call(GET, url)).toFuture
                  }

                case (ContinueRegistration.Delete, _) =>
                  for {
                    _ <- Future.fromTry(request.userAnswers.remove(PreviousUnfinishedRegistration))
                    _ <- saveAndComeBackService.deleteSavedUserAnswers(previousUserAnswers.journeyId)
                  } yield {
                    etmpIdType match
                      case VRN => Redirect(controllers.routes.CheckVatDetailsController.onPageLoad().url)
                      case FTR => Redirect(controllers.routes.ClientBusinessNameController.onPageLoad().url)
                      case UTR => Redirect(controllers.routes.ClientBusinessAddressController.onPageLoad().url)
                      case NINO => Redirect(controllers.routes.ClientBusinessAddressController.onPageLoad().url)
                  }

                case _ =>
                  val exception = new IllegalStateException("Illegal value submitted and/or must have a saved page url to return to the saved journey")
                  logger.error(exception.getMessage, exception)
                  throw exception
              }
          )
        case None =>
          val exception = new IllegalStateException("Must have previous unfinished registration journey")
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  private def deleteAndRedirect(
                                 journeyId: String,
                                 redirectUrl: Result
                               )(implicit ec: ExecutionContext, request: DataRequest[_]): Future[Result] = {
    for {
      _ <- saveAndComeBackService.deleteSavedUserAnswers(journeyId)
    } yield redirectUrl
  }
}


