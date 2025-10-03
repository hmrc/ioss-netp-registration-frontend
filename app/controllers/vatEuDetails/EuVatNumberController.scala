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

package controllers.vatEuDetails

import controllers.GetCountry
import controllers.actions.*
import forms.vatEuDetails.EuVatNumberFormProvider
import models.{CountryWithValidationDetails, Index}
import pages.vatEuDetails.EuVatNumberPage
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.vatEuDetails.EuVatNumberView
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatNumberController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: EuVatNumberFormProvider,
                                       view: EuVatNumberView,
                                       coreRegistrationValidationService: CoreRegistrationValidationService,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      getCountryWithIndex(waypoints, countryIndex) { country =>

        val form: Form[String] = formProvider(country)

        val preparedForm = request.userAnswers.get(EuVatNumberPage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
          case countryWithValidationDetails: CountryWithValidationDetails =>

            Ok(view(preparedForm, waypoints, countryIndex, countryWithValidationDetails)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.identifyAndGetData().async {
    implicit request =>

      val quarantineCutOffDate = LocalDate.now(clock).minusYears(2)
      
      getCountryWithIndex(waypoints, countryIndex) { country =>

        CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
          case countryWithValidationDetails: CountryWithValidationDetails =>

            val form: Form[String] = formProvider(country)

            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(formWithErrors, waypoints, countryIndex, countryWithValidationDetails)).toFuture,

              euVrn =>
                coreRegistrationValidationService.searchEuVrn(euVrn, country.code).flatMap {
                  case Some(activeMatch) if activeMatch.matchType.isActiveTrader && !activeMatch.traderId.isAnIntermediary =>
                    Redirect(controllers.routes.ClientAlreadyRegisteredController.onPageLoad()).toFuture

                  case Some(activeMatch) if activeMatch.matchType.isQuarantinedTrader &&
                    LocalDate.parse(activeMatch.getEffectiveDate).isAfter(quarantineCutOffDate) &&
                    !activeMatch.traderId.isAnIntermediary =>
                    Redirect(
                      controllers.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                        activeMatch.memberState,
                        activeMatch.getEffectiveDate)
                    ).toFuture

                  case _ =>
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage(countryIndex), euVrn))
                      _ <- cc.sessionRepository.set(updatedAnswers)
                    } yield Redirect(EuVatNumberPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
                }
            )
        }
      }
  }
}
