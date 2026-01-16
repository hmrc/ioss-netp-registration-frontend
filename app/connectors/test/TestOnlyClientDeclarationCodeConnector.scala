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

package connectors.test

import config.Service
import connectors.test.TestOnlyClientDeclarationCodeHttpParser.{TestOnlyValidateClientCodeReads, TestOnlyValidateClientCodeResponse}
import logging.Logging
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyClientDeclarationCodeConnector @Inject()(
                                                        httpClientV2: HttpClientV2,
                                                        config: Configuration
                                                      )(implicit ec: ExecutionContext) extends Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-netp-registration")

  def getTestOnlyCode(urlCode: String)(implicit hc: HeaderCarrier): Future[TestOnlyValidateClientCodeResponse] =
    httpClientV2
      .get(url"$baseUrl/test-only/ActivationCode/$urlCode")
      .execute[TestOnlyValidateClientCodeResponse]
}
