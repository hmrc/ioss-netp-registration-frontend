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

package controllers.saveAndComeBack

import connectors.{RegistrationConnector, SaveForLaterConnector}
import controllers.actions.*
import forms.ContinueRegistrationFormProvider
import models.ContinueRegistration
import pages.{ClientVatNumberPage, JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.SaveAndComeBackService
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationView
import models.saveAndComeBack.TaxReferenceInformation

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ContinueRegistrationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: ContinueRegistrationFormProvider,
                                                saveForLaterConnector: SaveForLaterConnector,
                                                view: ContinueRegistrationView,
                                                registrationConnector: RegistrationConnector,
                                                saveAndComeBackService: SaveAndComeBackService
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      request.userAnswers.get(ClientVatNumberPage) match {
        case None => {
          val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)

          request.userAnswers.get(SavedProgressPage).map { _ =>
            Ok(view(taxReferenceInformation, form, waypoints)).toFuture
          }.getOrElse {
            Redirect(controllers.routes.IndexController.onPageLoad()).toFuture // TODO- SCG- Change the index
          }
        }

        case Some(clientVatNumber) =>

          saveAndComeBackService.getVatTaxInfo(clientVatNumber, waypoints).map {
            case Left(call) => Redirect(call)
            case Right(vatInfo) =>
              val updatedAnswers = request.userAnswers.copy(vatInfo = Some(vatInfo))
              cc.sessionRepository.set(updatedAnswers)

              val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(updatedAnswers)

              request.userAnswers.get(SavedProgressPage).map { _ =>
                Ok(view(taxReferenceInformation, form, waypoints))
              }.getOrElse {
                Redirect(controllers.routes.IndexController.onPageLoad()) // TODO- SCG- Change the index
              }
          }
      }

  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      val taxReferenceInformation: TaxReferenceInformation = saveAndComeBackService.determineTaxReference(request.userAnswers)
      
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(taxReferenceInformation, formWithErrors, waypoints)).toFuture,

        value1 =>
          (value1, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) =>
              Redirect(Call(GET, url)).toFuture

            case (ContinueRegistration.Delete, _) =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveForLaterConnector.delete(taxReferenceInformation.journeyId)
              } yield Redirect(controllers.routes.IndexController.onPageLoad())

            case _ =>
              Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
          }
      )

  }
}
