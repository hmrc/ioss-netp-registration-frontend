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
import forms.DeclarationFormProvider
import models.UserAnswers
import models.requests.DataRequest

import javax.inject.Inject
import pages.{ClientBusinessNamePage, DeclarationPage, EmptyWaypoints, JourneyRecoveryPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DeclarationView
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DeclarationFormProvider,
                                         registrationConnector: RegistrationConnector,
                                         view: DeclarationView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetClientCompanyName {

  val form: Form[Boolean] = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

     getOrganisationName(request.userAnswers) match {
       case Some(clientCompanyName) =>
         getIntermediaryName().map { intermediaryOpt =>
           val intermediaryName = intermediaryOpt.getOrElse("")

           val preparedForm = request.userAnswers.get(DeclarationPage) match {
             case None => form
             case Some(value) => form.fill(value)
           }

           Ok(view(preparedForm, intermediaryName, clientCompanyName))
         }
       case None =>
         Redirect(JourneyRecoveryPage.route(EmptyWaypoints)).toFuture
     }
  }

  def onSubmit(): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getOrganisationName(request.userAnswers) match {
        case Some(clientCompanyName) =>
          getIntermediaryName().flatMap { intermediaryOpt =>
            val intermediaryName = intermediaryOpt.getOrElse("")

            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, intermediaryName, clientCompanyName))),

              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(DeclarationPage, value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(routes.ApplicationCompleteController.onPageLoad())
            )
          }
        case None =>
          Redirect(JourneyRecoveryPage.route(EmptyWaypoints)).toFuture
      }
  }

  private def getIntermediaryName()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    registrationConnector.getIntermediaryVatCustomerInfo().map {
      case Right(vatInfo) =>
        vatInfo.organisationName.orElse(vatInfo.individualName)
      case _ =>
        None
    }
  }

  private def getOrganisationName(answers: UserAnswers)(implicit request: DataRequest[_]): Option[String] = {
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => request.userAnswers.get(ClientBusinessNamePage).map { companyName =>
        companyName.name
      }
    }
  }
}
