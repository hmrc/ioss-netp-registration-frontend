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
import logging.Logging
import play.api.Configuration
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyPendingClientsConnector @Inject()(
                                                 httpClientV2: HttpClientV2,
                                                 config: Configuration
                                               )(implicit ec: ExecutionContext) extends Logging {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-netp-registration")

  def deletePendingRegistrations()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClientV2
      .delete(url"$baseUrl/test-only/delete-pending-registrations")
      .execute[HttpResponse]

  def createPendingRegistrations(intermediaryNumber: String, amount: Int)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClientV2
      .put(url"$baseUrl/test-only/create-pending-registrations/$intermediaryNumber/$amount")
      .execute[HttpResponse]
}
