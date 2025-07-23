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

package controllers.test

import connectors.test.TestOnlyClientDeclarationCodeConnector
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import logging.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyClientDeclarationCodeController @Inject()(
                                                         testOnlyClientDeclarationConnector: TestOnlyClientDeclarationCodeConnector,
                                                         identify: IdentifierAction,
                                                         getData: DataRetrievalAction,
                                                         val controllerComponents: MessagesControllerComponents,
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging{

def testOnlyGetClientDeclarationCode(urlCode: String): Action[AnyContent] = (identify andThen getData).async {
  implicit request =>
    testOnlyClientDeclarationConnector.getTestOnlyCode(urlCode).flatMap {
      case Right(response) =>
        Future.successful(Ok(s"""<p id="testOnlyDeclarationCode">$response</p>"""))
      case Left(error) =>
        val message: String = s"Received an unexpected error when trying to retrieve the declaration code: $error."
        val exception: Exception = new Exception(message)
        logger.error(exception.getMessage, exception)
        throw exception
    }
}
}