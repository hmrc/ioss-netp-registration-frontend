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

import config.FrontendAppConfig
import controllers.actions.*
import formats.Format.dateFormatter
import logging.Logging
import models.UserAnswers
import models.requests.DataRequest
import pages.ClientBusinessNamePage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ClientAlreadyRegisteredView

import java.time.LocalDate
import javax.inject.Inject

class ClientAlreadyRegisteredController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   frontendAppConfig: FrontendAppConfig,
                                                   view: ClientAlreadyRegisteredView
                                                 ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(exclusionEffectiveDate: String): Action[AnyContent] = cc.identifyAndGetData() {
    implicit request: DataRequest[_] =>

      val formattedExclusionEffectiveDate: String = LocalDate.parse(exclusionEffectiveDate).format(dateFormatter)

      getOrganisationName(request.userAnswers) match {
        case maybeClientCompanyName =>
          val companyName: String = maybeClientCompanyName.getOrElse(Messages("clientAlreadyRegistered.yourClient"))
          Ok(view(companyName, formattedExclusionEffectiveDate, frontendAppConfig.intermediaryYourAccountUrl))
      }
  }

  private def getOrganisationName(answers: UserAnswers)(implicit request: DataRequest[_]): Option[String] = {
    logger.info(s"vatInfo: ${answers.vatInfo}")
    logger.info(s"ClientBusinessNamePage: ${request.userAnswers.get(ClientBusinessNamePage)}")
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => request.userAnswers.get(ClientBusinessNamePage).map { companyName =>
        companyName.name
      }
    }
  }
}
