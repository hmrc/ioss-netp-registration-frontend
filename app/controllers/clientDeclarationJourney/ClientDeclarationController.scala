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

package controllers.clientDeclarationJourney

import connectors.RegistrationConnector
import controllers.actions.*
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import logging.Logging
import models.UserAnswers
import pages.clientDeclarationJourney.ClientDeclarationPage
import pages.{BusinessContactDetailsPage, ClientBusinessNamePage, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.IntermediaryStuffQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.clientDeclarationJourney.ClientDeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientDeclarationController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             unidentifiedDataRetrievalAction: ClientIdentifierAction,
                                             formProvider: ClientDeclarationFormProvider,
                                             registrationConnector: RegistrationConnector,
                                             clientDataRetrievalAction: ClientDataRetrievalAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             cc: AuthenticatedControllerComponents,
                                             view: ClientDeclarationView
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.clientIdentify andThen cc.clientGetData).async {
    implicit request =>
      
      getClientCompanyName(waypoints, request.userAnswers) { clientCompanyName =>
        getIntermediaryName(waypoints, request.userAnswers) { intermediaryName =>

          val preparedForm = request.userAnswers.get(ClientDeclarationPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, clientCompanyName, intermediaryName)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (cc.clientIdentify andThen cc.clientGetData).async {
    implicit request =>

      getClientCompanyName(waypoints, request.userAnswers) { clientCompanyName =>
        getIntermediaryName(waypoints, request.userAnswers) { intermediaryName =>

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, clientCompanyName, intermediaryName)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientDeclarationPage, value))
                _ <- sessionRepository.set(updatedAnswers)
              } yield Redirect(routes.ClientSuccessfulRegistrationController.onPageLoad())
          )
        }
      }
  }

  private def getIntermediaryName(waypoints: Waypoints, userAnswers: UserAnswers)(block: String => Future[Result]): Future[Result] = {
    userAnswers.get(IntermediaryStuffQuery).map { intermediaryStuff =>
      block(intermediaryStuff.intermediaryName)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)

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
}
