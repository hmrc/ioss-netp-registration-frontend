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

import connectors.RegistrationConnector
import controllers.actions.*
import logging.Logging
import models.UserAnswers
import models.vatEuDetails.EuDetails
import pages.vatEuDetails.HasFixedEstablishmentPage

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.{DeleteAllFixedEstablishmentPage, Waypoints}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{IossNumberQuery, OriginalRegistrationQuery}
import queries.euDetails.AllEuDetailsQuery
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.DeleteAllFixedEstablishmentView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DeleteAllFixedEstablishmentController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       cc: AuthenticatedControllerComponents,
                                                       view: DeleteAllFixedEstablishmentView,
                                                       registrationConnector: RegistrationConnector,
                                                       registrationService: RegistrationService
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(
                  waypoints: Waypoints,
                  iossNumber: String
                ): Action[AnyContent] = cc.identifyAndGetOptionalData(inAmend = true, iossNumber = Some(iossNumber)).async {
    implicit request =>

      registrationConnector.displayRegistrationNetp(iossNumber).flatMap {
        case Right(registrationWrapper) =>
          for {
            userAnswers <- registrationService.toUserAnswers(request.userId, registrationWrapper)
            answersWithFixedEstablishments <- Future.fromTry(userAnswers.set(
              AllEuDetailsQuery,
              registrationService.convertEuFixedEstablishmentDetails(
                  registrationWrapper
                    .etmpDisplayRegistration
                    .schemeDetails
                    .euRegistrationDetails
                )
                .toList
            ))
            answersWithIossNumber <- Future.fromTry(answersWithFixedEstablishments.set(IossNumberQuery, iossNumber))
            originalAnswers <- Future.fromTry(answersWithIossNumber.set(OriginalRegistrationQuery(iossNumber), registrationWrapper.etmpDisplayRegistration))
            _ <- cc.sessionRepository.set(originalAnswers)
          } yield {

            val fixedEstablishments = getEuDetailsForFixedEstablishments(originalAnswers)

            Ok(view(fixedEstablishments, waypoints, iossNumber))
          }
        case Left(error) =>
          val exception = new Exception(error.body)
          logger.error(exception.getMessage, exception)
          Future.failed(exception)
      }
  }

  def onSubmit(
                waypoints: Waypoints,
                iossNumber: String
              ): Action[AnyContent] = cc.identifyAndGetOptionalData(inAmend = true, iossNumber = Some(iossNumber)).async {
    implicit request =>

      request.userAnswers match {
        case Some(userAnswers) =>
          for {
            updatedAnswers <- Future.fromTry(deleteFixedEstablishment(userAnswers))
            _ <- cc.sessionRepository.set(updatedAnswers)

            registrationWrapper <- registrationConnector
              .displayRegistrationNetp(iossNumber)
              .flatMap {
                case Right(wrapper) =>
                  Future.successful(wrapper)

                case Left(error) =>
                  Future.failed(new Exception(error.body))
              }

            amendResult <- registrationService.amendRegistration(
              answers = updatedAnswers,
              registration = registrationWrapper.etmpDisplayRegistration,
              iossNumber = iossNumber
            )
          } yield {
            amendResult match {
              case Right(_) =>
                Redirect(DeleteAllFixedEstablishmentPage(iossNumber).navigate(waypoints, userAnswers, updatedAnswers).route)

              case Left(error) =>
                logger.error(s"Unable to remove fixed establishments: ${error.body}")

                Redirect(controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad())
            }
          }
        case None =>
          Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
      }
  }

  private def getEuDetailsForFixedEstablishments(userAnswers: UserAnswers): Seq[EuDetails] = {
    userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty).filter(_.hasFixedEstablishment.contains(true))
  }

  private def deleteFixedEstablishment(userAnswers: UserAnswers): Try[UserAnswers] = {

    for {
      answersWithoutEuDetails <- userAnswers.set(AllEuDetailsQuery, List.empty[EuDetails])
      updatedAnswers <- answersWithoutEuDetails.set(HasFixedEstablishmentPage, false)
    } yield updatedAnswers
  }
}
