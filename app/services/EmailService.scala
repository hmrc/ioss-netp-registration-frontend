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

package services

import config.Constants.clientDeclarationEmailTemplateId
import connectors.EmailConnector
import models.emails.{ClientDeclarationEmailParameters, EmailSendingResult, EmailToSendRequest}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class EmailService @Inject()(
                              emailConnector: EmailConnector
                            )(implicit executionContext: ExecutionContext) {


  def sendClientActivationEmail(
                                 intermediary_name: String,
                                 recipientName_line1: String,
                                 activation_code: String,
                                 activation_code_expiry_date: String,
                                 emailAddress: String,
                               )(implicit hc: HeaderCarrier, messages: Messages): Future[EmailSendingResult] = {

    val emailParameters =
      ClientDeclarationEmailParameters(
        intermediary_name,
        recipientName_line1,
        activation_code,
        activation_code_expiry_date
      )

    emailConnector.send(
      EmailToSendRequest(
        List(emailAddress),
        clientDeclarationEmailTemplateId,
        emailParameters
      )
    )
  }

}

