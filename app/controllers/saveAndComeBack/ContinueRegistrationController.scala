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

package controllers.saveAndComeBack

import config.FrontendAppConfig
import controllers.SetActiveTraderResult
import controllers.actions.*
import forms.saveAndComeBack.ContinueRegistrationFormProvider
import logging.Logging
import models.core.Match
import models.domain.VatCustomerInfo
import models.requests.DataRequest
import models.saveAndComeBack.{ContinueRegistration, TaxReferenceInformation}
import pages.{ClientUtrNumberPage, ClientVatNumberPage, Page, SavedProgressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.Gettable
import services.SaveAndComeBackService
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: ContinueRegistrationFormProvider,
                                                view: ContinueRegistrationView,
                                                frontendAppConfig: FrontendAppConfig,
                                                saveAndComeBackService: SaveAndComeBackService,
                                                coreRegistrationValidationService: CoreRegistrationValidationService,
                                                clock: Clock
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with SetActiveTraderResult {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      request.userAnswers.get(ClientVatNumberPage) match {
        case None =>
          val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)

          request.userAnswers.get(SavedProgressPage).map { _ =>
            Ok(view(taxReferenceInformation, form, waypoints)).toFuture
          }.getOrElse {
            val exception = new IllegalStateException("Must have a saved page url to return to the saved journey")
            logger.error(exception.getMessage, exception)
            throw exception
          }

        case Some(clientVatNumber) =>

          saveAndComeBackService.getVatTaxInfo(clientVatNumber, waypoints).map { vatCustomerInfo =>
            val updatedAnswers = request.userAnswers.copy(vatInfo = Some(vatCustomerInfo))
            cc.sessionRepository.set(updatedAnswers)

            val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(updatedAnswers)

            request.userAnswers.get(SavedProgressPage).map { _ =>
              Ok(view(taxReferenceInformation, form, waypoints))
            }.getOrElse {
              val exception = new IllegalStateException("Must have a saved page url to return to the saved journey")
              logger.error(exception.getMessage, exception)
              throw exception
            }
          }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)
      val dashboardUrl = frontendAppConfig.intermediaryYourAccountUrl

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(taxReferenceInformation, formWithErrors, waypoints)).toFuture,

        value1 =>
          (value1, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) =>
              // TODO -> Call new service method checkAndValidateSavedUserAnswers

              // TODO -> Check these redirects
              val alreadyRegisteredRedirect = controllers.routes.ClientAlreadyRegisteredController.onPageLoad().url

              deleteAndRedirect(taxReferenceInformation, alreadyRegisteredRedirect)

            case (ContinueRegistration.Delete, _)
            =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveAndComeBackService.deleteSavedUserAnswers(taxReferenceInformation.journeyId)
              } yield Redirect(dashboardUrl)

            case _ =>
              val exception = new IllegalStateException("Illegal value submitted and/or must have a saved page url to return to the saved journey")
              logger.error(exception.getMessage, exception)
              throw exception
          }

      )
  }

  private def checkVatExpired(vatCustomerInfo: Option[VatCustomerInfo]): Boolean = {
    vatCustomerInfo match {
      case Some(vatCustomerInfo) =>
        val today: LocalDate = LocalDate.now(clock)
        vatCustomerInfo.deregistrationDecisionDate.exists(!_.isAfter(today))

      case None =>
        val message: String = "VAT Info not retrieved"
        logger.warn(message)
        val exception: Exception = new Exception(message)
        throw exception
    }
  }


  private def deleteAndRedirect(
                                 taxReferenceInformation: TaxReferenceInformation,
                                 redirectUrl: String
                               )(implicit ec: ExecutionContext, request: DataRequest[_]): Future[Result] = {
    for {
      _ <- cc.sessionRepository.clear(request.userId)
      _ <- saveAndComeBackService.deleteSavedUserAnswers(taxReferenceInformation.journeyId)
    } yield Redirect(redirectUrl)
  }


  // ClientVatNumberPage -> searchUkVrn
  // ClientUtrNumberPage -> searchTraderId
  // ClientsNinoNumberPage -> searchTraderId
  // ClientTaxReferencePage -> searchForeignTaxReference

  // USE THE MODELS FOR THESE -> LOOK AT IOSS-REG
  // EuTaxReferencePage -> searchEuTaxId
  // EuVatNumberPage -> searchEuVrn
  // PreviousIossNumberPage -> searchScheme
  // PreviousOssNumberPage -> searchScheme


  //  private def determineSearchType[A](
  //                                   pages: List[Gettable[A]]
  //                                 )(implicit hc: HeaderCarrier, request: DataRequest[_], rds: Reads[A]) = {
  //    for {
  //      page <- pages
  //    } yield {
  //      if (request.userAnswers.get(page).nonEmpty) {
  //        val x = request.userAnswers.get(page)
  //        page match {
  //          case _: ClientVatNumberPage.type =>
  //            coreRegistrationValidationService.searchUkVrn()
  //
  //          case _: ClientUtrNumberPage.type =>
  //              coreRegistrationValidationService.searchTraderId()
  //        }
  //      } else {
  //        // TODO -> Nothing in page
  //        ???
  //      }
  //    }
  //  }
}


