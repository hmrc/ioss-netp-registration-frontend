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
import forms.ContinueRegistrationSelectionFormProvider
import logging.Logging
import models.UserAnswers
import models.saveAndComeBack.{MultipleRegistrations, NoRegistrations, SingleRegistration}
import pages.{ContinueRegistrationSelectionPage, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
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
                                                         saveForLaterConnector: SaveForLaterConnector,
                                                         formProvider: ContinueRegistrationSelectionFormProvider,
                                                         saveAndComeBackService: SaveAndComeBackService,
                                                         view: ContinueRegistrationSelectionView
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>

      val userAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

      saveAndComeBackService.scgSelectTheClassSCG(userAnswers, request.intermediaryNumber.get).flatMap {
        case SingleRegistration(singleJourneyId) =>

          saveAndComeBackService.retrieveSingleSavedUserAnswers(singleJourneyId, waypoints).flatMap {

            case Right(enrichedUserAnswers) => {
              for {
                _ <- cc.sessionRepository.set(enrichedUserAnswers)
              } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())
            }
            case Left(errorCall) => Redirect(errorCall).toFuture
          }

        case MultipleRegistrations(multipleRegistrations) =>
          saveAndComeBackService.fetchOutcomesFailFast(multipleRegistrations).map { seqTaxReferenceInfo =>
            Ok(view(seqTaxReferenceInfo, form, waypoints))
          }

        case NoRegistrations => ???

        case _ => ???
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

