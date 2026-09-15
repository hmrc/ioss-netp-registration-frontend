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
import controllers.saveAndComeBack.routes
import models.{Index, UserAnswers}
import pages.vatEuDetails.HasFixedEstablishmentPage
import pages.website.WebsitePage

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.{ClientBusinessNamePage, JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.AllWebsites
import queries.euDetails.AllEuDetailsQuery
import services.SaveAndComeBackService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.RemovingClientsFixedEstablishmentDetailsView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RemovingClientsFixedEstablishmentDetailsController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: RemovingClientsFixedEstablishmentDetailsView,
                                       saveAndComeBackService: SaveAndComeBackService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.identify andThen cc.getData andThen cc.requireData()).async {
    implicit request =>

      getClientCompanyName(waypoints, request.userAnswers) { clientCompanyName =>

        Ok(view(clientCompanyName, waypoints)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (cc.identify andThen cc.getData andThen cc.requireData()).async {
    implicit request =>

      getClientCompanyName(waypoints, request.userAnswers) { clientCompanyName =>
        for {
          updatedAnswers <- Future.fromTry(deleteFixedEstablishment(request.userAnswers, waypoints))
          _ <- cc.sessionRepository.set(updatedAnswers)
          _ <- saveAndComeBackService.updateSavedUserAnswers(updatedAnswers, request.intermediaryNumber)

        } yield {
          Redirect(routes.ContinueRegistrationController.onPageLoad(waypoints))
        }
      }
  }

  private def getClientCompanyName(waypoints: Waypoints, userAnswers: UserAnswers)
                                  (block: String => Future[Result]): Future[Result] = {
    userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>
        vatCustomerInfo.organisationName match {
          case Some(orgName) => block(orgName)
          case _ =>
            vatCustomerInfo.individualName
              .map(block)
              .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
        }

      case _ =>
        userAnswers.get(ClientBusinessNamePage).map { clientBusinessNamePage =>
          block(clientBusinessNamePage.name)
        }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
    }
  }

  private def deleteFixedEstablishment(userAnswers: UserAnswers, waypoints: Waypoints): Try[UserAnswers] = {

    val websiteIndex = Index(userAnswers.get(AllWebsites).map(_.size).getOrElse(0))
      
    for {
      withoutEuDetails <- userAnswers.set(AllEuDetailsQuery, List.empty)
      answersWithoutFixedEstablishments <- withoutEuDetails.set(HasFixedEstablishmentPage, false)
      updateAnswers <- answersWithoutFixedEstablishments.set(SavedProgressPage, WebsitePage(websiteIndex).route(waypoints).url)
    } yield updateAnswers
  }
}
