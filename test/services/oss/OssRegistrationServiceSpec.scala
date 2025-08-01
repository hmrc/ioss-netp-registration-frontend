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

package services.oss

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.ossRegistration.OssRegistration
import models.responses.RegistrationNotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class OssRegistrationServiceSpec extends SpecBase with PrivateMethodTester with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val arbOssRegistration: OssRegistration = arbitraryOssRegistration.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockConfig)
  }

  "OssRegistrationService" - {

    ".getLatestOssRegistration" - {

      "must return an OssRegistration when the connector returns a a valid payload" in {

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(arbOssRegistration).toFuture
        when(mockConfig.ossEnrolment) thenReturn "HMRC-OSS-ORG"

        val service = OssRegistrationService(mockRegistrationConnector, mockConfig)

        val result = service.getLatestOssRegistration(vrn).futureValue

        result mustBe arbOssRegistration
      }

      "must throw an exception when connector returns Left(Error)" in {

        when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Left(RegistrationNotFound).toFuture
        when(mockConfig.ossEnrolment) thenReturn "HMRC-OSS-ORG"

        val service = OssRegistrationService(mockRegistrationConnector, mockConfig)

        val result = intercept[Exception] {
          service.getLatestOssRegistration(vrn).futureValue
        }

        result.getMessage must include(s"Failed to fetch OSS registration for VRN $vrn")
      }
    }
  }
}
