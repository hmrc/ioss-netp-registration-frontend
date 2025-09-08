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
import logging.Logging
import models.audit.DeclarationSigningAuditType.CreateDeclaration
import models.audit.SubmissionResult.{Failure, Success}
import models.audit.DeclarationSigningAuditModel
import models.audit.{DeclarationSigningAuditType, SubmissionResult}
import models.emails.EmailSendingResult
import models.{IntermediaryDetails, PendingRegistrationRequest, SavedPendingRegistration}
import pages.{DeclarationPage, ErrorSubmittingPendingRegistrationPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, EmailService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.GetClientEmail
import views.html.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: DeclarationFormProvider,
                                       registrationConnector: RegistrationConnector,
                                       auditService: AuditService,
                                       emailService: EmailService,
                                       view: DeclarationView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetClientCompanyName with Logging with GetClientEmail {

  val form: Form[Boolean] = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getClientCompanyName(waypoints) { clientCompanyName =>
        getIntermediaryName().map { intermediaryOpt =>
          val intermediaryName = intermediaryOpt.getOrElse("")

          val preparedForm = request.userAnswers.get(DeclarationPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, intermediaryName, clientCompanyName))
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      getClientCompanyName(waypoints) { clientCompanyName =>
        getIntermediaryName().flatMap { intermediaryOpt =>
          val intermediaryName = intermediaryOpt.getOrElse("")

          val pendingRegistrationRequest = PendingRegistrationRequest(request.userAnswers, IntermediaryDetails(request.intermediaryNumber, intermediaryName))

          registrationConnector.submitPendingRegistration(pendingRegistrationRequest).flatMap {
            case Right(submittedRegistration) =>

              getClientEmail(waypoints, submittedRegistration.userAnswers) { clientEmail =>
                sendEmail(submittedRegistration, clientEmail, clientCompanyName, intermediaryName)

                form.bindFromRequest().fold(
                  formWithErrors =>
                    BadRequest(view(formWithErrors, waypoints, intermediaryName, clientCompanyName)).toFuture,

                  value =>
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(DeclarationPage, value))
                      _ <- cc.sessionRepository.set(updatedAnswers)
                    } yield {
                      auditService.audit(
                        DeclarationSigningAuditModel.build(
                          declarationSigningAuditType = CreateDeclaration,
                          userAnswers = updatedAnswers,
                          submissionResult = Success,
                          submittedDeclarationPageBody = view(form.fill(value), waypoints, intermediaryName, clientCompanyName).body
                        )
                      )
                      Redirect(routes.ApplicationCompleteController.onPageLoad())
                    }
                )
              }

            case Left(error) =>
              auditService.audit(
                DeclarationSigningAuditModel.build(
                  declarationSigningAuditType = CreateDeclaration,
                  userAnswers = request.userAnswers,
                  submissionResult = Failure,
                  submittedDeclarationPageBody = view(form, waypoints, intermediaryName, clientCompanyName).body
                )
              )
              logger.error(s"Received an unexpected error when submitting the pending registration: ${error.body}")
              Redirect(ErrorSubmittingPendingRegistrationPage.route(waypoints).url).toFuture
          }
        }
      }

  }


  private def getIntermediaryName()(implicit hc: HeaderCarrier): Future[Option[String]] = {

    val futureResult = registrationConnector.getIntermediaryVatCustomerInfo()

    if (registrationConnector.getIntermediaryVatCustomerInfo() == null) {
      None.toFuture
    } else {
      futureResult.map {
        case Right(vatInfo) =>
          vatInfo.organisationName.orElse(vatInfo.individualName)

        case _ =>
          logger.error("Unable to retrieve an intermediary name as no Organisation name or Individual name is registered")
          throw new IllegalStateException("Unable to retrieve an intermediary name as no Organisation name or Individual name is registered")
      }
    }
  }

  private def sendEmail(
                         submittedRegistration: SavedPendingRegistration,
                         clientEmail: String,
                         clientCompanyName: String,
                         intermediaryName: String
                       )(implicit hc: HeaderCarrier, messages: Messages): Future[EmailSendingResult] = {

    emailService.sendClientActivationEmail(
      intermediary_name = intermediaryName,
      recipientName_line1 = clientCompanyName,
      activation_code_expiry_date = submittedRegistration.activationExpiryDate,
      activation_code = submittedRegistration.uniqueActivationCode,
      emailAddress = clientEmail
    )
  }
}