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
import connectors.SaveForLaterHttpParser.{
  DeleteSaveForLaterReads,
  DeleteSaveForLaterResponse,
  SaveForLaterHttpReads,
  SaveForLaterIntermediaryHttpReads,
  SaveForLaterIntermediaryResponse,
  SaveForLaterResponse
}
import models.SaveForLaterRequest
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterConnector @Inject()(
                                       httpClientV2: HttpClientV2,
                                       config: Configuration
                                     )(implicit executionContext: ExecutionContext) {

  private val baseUrl: Service = config.get[Service]("microservice.services.ioss-netp-registration")

  def submit(saveForLaterRequest: SaveForLaterRequest)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    httpClientV2.post(url"$baseUrl/save-for-later").withBody(Json.toJson(saveForLaterRequest)).execute[SaveForLaterResponse]
  }

  def getClientRegistration(journeyId: String)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    httpClientV2.get(url"$baseUrl/save-for-later/$journeyId").execute[SaveForLaterResponse]
  }

  def getAllByIntermediary(intermediaryNumber: String)(implicit hc: HeaderCarrier): Future[SaveForLaterIntermediaryResponse] = {
    httpClientV2.get(url"$baseUrl/save-for-later-selection/$intermediaryNumber").execute[SaveForLaterIntermediaryResponse]
  }

  def delete(journeyId: String)(implicit hc: HeaderCarrier): Future[DeleteSaveForLaterResponse] = {
    httpClientV2.get(url"$baseUrl/save-for-later/delete/$journeyId").execute[DeleteSaveForLaterResponse]
  }
}
