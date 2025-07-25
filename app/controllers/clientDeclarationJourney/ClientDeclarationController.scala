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
import controllers.GetClientCompanyName
import controllers.actions.*
import forms.clientDeclarationJourney.ClientDeclarationFormProvider
import logging.Logging
import pages.Waypoints
import pages.clientDeclarationJourney.ClientDeclarationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.clientDeclarationJourney.ClientDeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientDeclarationController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             sessionRepository: SessionRepository,
                                             unidentifiedDataRetrievalAction: UnidentifiedDataRetrievalAction,
                                             registrationConnector: RegistrationConnector,
                                             requiredAction: DataRequiredAction,
                                             formProvider: ClientDeclarationFormProvider,
                                             view: ClientDeclarationView
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetClientCompanyName with Logging {

  val form = formProvider()

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (unidentifiedDataRetrievalAction andThen requiredAction).async {
    implicit request =>

      println(s"\n\nrequest:\n")
      println(s"$request")
      println(s"\n\nrequest.userAnswers:\n")
      println(s"${request.userAnswers}")
      
      getClientCompanyName(waypoints) { clientCompanyName =>
        println(s"\n\ngetClientCompanyName:\n")
        println(s"$clientCompanyName")

        getIntermediaryName().map { intermediaryOpt =>
          val intermediaryName = intermediaryOpt.getOrElse("")

          println(s"\n\ngetIntermediaryName:\n")
          println(s"$intermediaryOpt")
          val preparedForm = request.userAnswers.get(ClientDeclarationPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, clientCompanyName, intermediaryName))
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (unidentifiedDataRetrievalAction andThen requiredAction).async {
    implicit request =>

      getClientCompanyName(waypoints) { clientCompanyName =>
        getIntermediaryName().flatMap { intermediaryOpt =>
          val intermediaryName = intermediaryOpt.getOrElse("")

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

  private def getIntermediaryName()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    registrationConnector.getIntermediaryVatCustomerInfo().map {
      case Right(vatInfo) =>
        vatInfo.organisationName.orElse(vatInfo.individualName)
      case _ =>
        logger.error("Unable to retrieve an intermediary name as no Organisation name or Individual name is registered")
        throw new IllegalStateException("Unable to retrieve an intermediary name as no Organisation name or Individual name is registered")
    }
  }

}
