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

import config.Constants.pendingRegistrationTTL
import connectors.RegistrationConnector
import controllers.actions.*
import controllers.routes
import formats.Format.dateFormatter
import forms.clientDeclarationJourney.ClientCodeEntryFormProvider
import logging.Logging
import models.UserAnswers
import pages.Waypoints
import pages.clientDeclarationJourney.ClientCodeEntryPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.GetClientEmail
import views.html.clientDeclarationJourney.ClientCodeEntryView

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientCodeEntryController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           sessionRepository: SessionRepository,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalAction,
                                           formProvider: ClientCodeEntryFormProvider,
                                           registrationConnector: RegistrationConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: ClientCodeEntryView
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with GetClientEmail {

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints, uniqueUrlCode: String): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      registrationConnector.getPendingRegistration(uniqueUrlCode).flatMap {
        case Right(savedPendingRegistration) =>
          val preparedForm = request.userAnswers.flatMap(_.get(ClientCodeEntryPage(uniqueUrlCode))) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          getClientEmail(waypoints, savedPendingRegistration.userAnswers) { clientEmail =>
            Ok(view(preparedForm, waypoints, clientEmail, uniqueUrlCode)).toFuture
          }


        case Left(errors) =>
          val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given journey ID: $errors."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }

  def onSubmit(waypoints: Waypoints, uniqueUrlCode: String): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      registrationConnector.getPendingRegistration(uniqueUrlCode).flatMap {
        case Right(savedPendingRegistration) =>

          getClientEmail(waypoints, savedPendingRegistration.userAnswers) { clientEmail =>

            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, waypoints, clientEmail, uniqueUrlCode))),

              value =>
                val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

                for {
                  updatedAnswers <- Future.fromTry(originalAnswers.set(ClientCodeEntryPage(uniqueUrlCode), value))
                  _ <- sessionRepository.set(updatedAnswers)
                } yield Redirect(ClientCodeEntryPage(uniqueUrlCode).navigate(waypoints, originalAnswers, updatedAnswers).route)
            )
          }
        case Left(errors) =>
          val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given journey ID: $errors."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }
}