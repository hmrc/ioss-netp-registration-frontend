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

import connectors.RegistrationConnector
import models.domain.{PreviousRegistration, VatCustomerInfo}
import models.intermediaries.{PreviousIntermediaryRegistration, StandardPeriod}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IntermediaryRegistrationService @Inject()(
                                         registrationConnector: RegistrationConnector
                                       )(implicit ec: ExecutionContext) {

  def getIntermediaryRegistration()(implicit hc: HeaderCarrier): Future[Option[VatCustomerInfo]] = {
      registrationConnector.getIntermediaryVatCustomerInfo().map {
        case Right(vatInfo) => Some(vatInfo)
        case Left(error) => None
      }
    
  }
  
  def getPreviousRegistrations()(implicit hc: HeaderCarrier): Future[Seq[PreviousIntermediaryRegistration]] = {
    registrationConnector.getPreviousIntermediaryAccounts().map { accounts =>

      val accountDetails: Seq[(YearMonth, String)] = accounts
        .enrolments.map(e => e.activationDate -> e.identifiers.find(_.key == "IntNumber").map(_.value))
        .collect {
          case (Some(activationDate), Some(intermediaryNumber)) => YearMonth.from(activationDate) -> intermediaryNumber
        }.sortBy(_._1)

      accountDetails.zip(accountDetails.drop(1)).map { case ((activationDate, intermediaryNumber), (nextActivationDate, _)) =>
        PreviousIntermediaryRegistration(
          startPeriod = StandardPeriod(activationDate),
          endPeriod = StandardPeriod(nextActivationDate.minusMonths(1)),
          intermediaryNumber = intermediaryNumber
        )
      }
    }
  }
}
