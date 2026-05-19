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

package controllers.test

import connectors.test.TestOnlyPendingClientsConnector
import logging.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyPendingClientsController @Inject()(
                                                  testOnlyPendingClientsConnector: TestOnlyPendingClientsConnector,
                                                  val controllerComponents: MessagesControllerComponents,
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def testOnlyDeletePendingRegistrations(): Action[AnyContent] = Action.async {
    implicit request =>
      testOnlyPendingClientsConnector.deletePendingRegistrations().map { _ =>
        NoContent
      }
  }

  def testOnlyCreatePendingRegistration(intermediaryNumber: String, amount: Int): Action[AnyContent] = Action.async {
    implicit request =>
      testOnlyPendingClientsConnector.createPendingRegistrations(intermediaryNumber, amount).map { _ =>
        NoContent
      }
  }
}