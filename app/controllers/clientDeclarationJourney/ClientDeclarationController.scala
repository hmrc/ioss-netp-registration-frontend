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

import controllers.actions.*
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import logging.Logging
import models.UserAnswers
import models.audit.DeclarationSigningAuditType.CreateClientDeclaration
import models.audit.SubmissionResult
import pages.clientDeclarationJourney.ClientDeclarationPage
import pages.{ClientBusinessNamePage, ErrorSubmittingRegistrationPage, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.IntermediaryDetailsQuery
import queries.etmp.EtmpEnrolmentResponseQuery
import repositories.SessionRepository
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.clientDeclarationJourney.ClientDeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientDeclarationController @Inject()(
                                             cc: AuthenticatedControllerComponents,
                                             sessionRepository: SessionRepository,
                                             formProvider: ClientDeclarationFormProvider,
                                             auditService: AuditService,
                                             clientValidationFilter: ClientValidationFilterProvider,
                                             registrationService: RegistrationService,
                                             view: ClientDeclarationView
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.clientIdentify andThen cc.clientGetData andThen clientValidationFilter.apply()).async {
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
              registrationService.createRegistration(request.userAnswers).flatMap {
                case Right(response) =>
                  auditService.sendAudit(
                    declarationSigningAuditType = CreateClientDeclaration,
                    result = SubmissionResult.Success,
                    submittedDeclarationPageBody = view(form, waypoints, intermediaryName, clientCompanyName).body
                  )
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ClientDeclarationPage, value))
                    updatedAnswers2 <- Future.fromTry(updatedAnswers.set(EtmpEnrolmentResponseQuery, response))
                    _ <- sessionRepository.set(updatedAnswers2)
                  } yield Redirect(routes.ClientSuccessfulRegistrationController.onPageLoad())


                case Left(error) =>
                  auditService.sendAudit(
                    declarationSigningAuditType = CreateClientDeclaration,
                    result = SubmissionResult.Failure,
                    submittedDeclarationPageBody = view(form, waypoints, intermediaryName, clientCompanyName).body
                  )
                  logger.error(s"Unexpected result on registration creation submission: ${error.body}")
                  Redirect(ErrorSubmittingRegistrationPage.route(waypoints)).toFuture
              }
          )
        }
      }
  }

  private def getIntermediaryName(waypoints: Waypoints, userAnswers: UserAnswers)(block: String => Future[Result]): Future[Result] = {
    userAnswers.get(IntermediaryDetailsQuery).map { intermediaryDetails =>
      block(intermediaryDetails.intermediaryName)
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
