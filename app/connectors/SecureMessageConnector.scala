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

package connectors

import config.Service
import connectors.SecureMessagesHttpParser.{SecureMessageResultResponse, SecureMessageResultResponseReads}
import logging.Logging
import models.securemessage.{CustomerEnrolment, MessageFilter}
import play.api.Configuration
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecureMessageConnector @Inject()(
                                        httpClientV2: HttpClientV2,
                                        config: Configuration
                                      )(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.secure-message")

  def getMessages(
                   enrolmentKey: Option[String] = None,
                   enrolment: Option[CustomerEnrolment] = None,
                   messageFilter: Option[MessageFilter] = None,
                   language: Option[String] = None,
                   taxIdentifiers: Option[String] = None
                 )(implicit hc: HeaderCarrier): Future[SecureMessageResultResponse] = {

    val queryParams = Seq(
      enrolmentKey.map("enrolmentKey" -> _),
      enrolment.map(e => "enrolment" -> e.toQueryParam),
      messageFilter.map(e => "messageFilter" -> e.toQueryParam),
      language.map("language" -> _),
      taxIdentifiers.map("taxIdentifiers" -> _)
    ).flatten
    
    httpClientV2.get(url"$baseUrl/messages")
      .transform(_.addQueryStringParameters(queryParams: _*))
      .execute[SecureMessageResultResponse]
  }
}
