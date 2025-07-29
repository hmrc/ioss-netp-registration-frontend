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
import controllers.actions.ClientIdentifierAction
import logging.Logging
import models.UserAnswers
import models.domain.VatCustomerInfo
import pages.{BusinessContactDetailsPage, Waypoints}
import pages.clientDeclarationJourney.ClientCodeEntryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import models.IntermediaryStuff
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.IntermediaryStuffQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class ClientJourneyStartController @Inject()(
                                              unidentifiedDataRetrievalAction: ClientIdentifierAction,
                                              registrationConnector: RegistrationConnector,
                                              sessionRepository: SessionRepository,
                                              val controllerComponents: MessagesControllerComponents,
                                            )(implicit executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(waypoints: Waypoints, uniqueUrlCode: String): Action[AnyContent] = unidentifiedDataRetrievalAction.async {
    implicit request =>

      println("✅ HTTP Method: " + request.method)
      println("✅ Path: " + request.path)
      println("✅ Headers: " + request.headers)
      println("✅ Query Params: " + request.queryString)
      println("✅ Session: " + request.session)

      Ok("Check logs for request details.")
      registrationConnector.getPendingRegistration(uniqueUrlCode).flatMap {
        case Right(savedPendingRegistration) =>

          //Create new or find old userAnswers
          val clientUserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))

          for {
            businessContactDetailsIntermediary <- Future.fromTry(
              savedPendingRegistration.userAnswers.get(BusinessContactDetailsPage)
                .toRight(new IllegalStateException("Missing client business details information"))
                .toTry
            )
            //Set Business contact details for later
            clientWithBusinessContactDetails <- Future.fromTry(clientUserAnswers.set(BusinessContactDetailsPage, businessContactDetailsIntermediary))
            //Set Intermediary details for later
            updatedAnswers <- Future.fromTry(clientWithBusinessContactDetails.set(IntermediaryStuffQuery, IntermediaryStuff(savedPendingRegistration.intermediaryStuff.intermediaryNumber, savedPendingRegistration.intermediaryStuff.intermediaryName)))
            //Update sessionRepository with answers
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.ClientCodeEntryController.onPageLoad(waypoints, uniqueUrlCode))

// TODO SCG-> 
          //Intermediary number comes from the enrolments during the intermediary journey. As such it is needed in DataRequired to use in the request.
          // But if we do not add it to the optional etc. then we can make a emthod to .get(Intermediary stuff) instead
        case Left(error) =>
          val message: String = s"Received an unexpected error when trying to retrieve a pending registration for the given unique Url Code: $uniqueUrlCode, \n Errors: $error."
          val exception: Exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }

  }

}
