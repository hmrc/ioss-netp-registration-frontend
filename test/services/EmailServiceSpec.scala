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

package services

import base.SpecBase
import connectors.EmailConnector
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.emails.{ClientDeclarationEmailParameters, EmailToSendRequest}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit val messages: Messages = Helpers.stubMessages(
    Helpers.stubMessagesApi(Map("en" -> Map("site.to" -> "to"))))
  private val connector = mock[EmailConnector]
  private val emailService = new EmailService(connector)
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "EmailService.sendClientActivationEmail" - {

    "when calling sendClientActivationEmail with the template id ioss_netp_email_declaration_code and the correct parameters" in {
      val maxLengthContactName = 105
      val maxLengthBusiness = 160
      val activationCodeExpiryDate = LocalDate.of(2022, 10, 1)

      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, intermediaryName: String, clientName: String) =>

          val expectedActivationCodeExpiryDate = activationCodeExpiryDate.format(formatter)

          val expectedEmailToSendRequest = EmailToSendRequest(
            to = List(email),
            templateId = "ioss_netp_email_declaration_code",
            parameters = ClientDeclarationEmailParameters(
              intermediary_name = intermediaryName,
              recipientName_line1 = clientName,
              activation_code = "12345",
              activation_code_expiry_date = expectedActivationCodeExpiryDate
            )
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendClientActivationEmail(
            intermediary_name = intermediaryName,
            recipientName_line1 = clientName,
            activation_code = "12345",
            activation_code_expiry_date = expectedActivationCodeExpiryDate,
            emailAddress = email
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }
  }
}