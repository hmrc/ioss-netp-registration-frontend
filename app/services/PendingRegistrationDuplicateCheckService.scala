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
import models.SavedPendingRegistration
import models.etmp.EtmpIdType
import pages.Waypoints
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PendingRegistrationDuplicateCheckService @Inject()(
                                         registrationConnector: RegistrationConnector
                                       )(implicit ec: ExecutionContext) {

  def checkPendingRegistration(
                                idType: EtmpIdType,
                                idValue: String,
                                intermediaryNumber: String,
                                waypoints: Waypoints
                              )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
      registrationConnector.getPendingRegistrationsByCustomerIdentification(idType, idValue).map {
        case Right(pendingRegistrations) =>
          pendingRegistrations.find(_.intermediaryDetails.intermediaryNumber == intermediaryNumber) match {
            case Some(pendingRegistration) =>
              Some(Redirect(controllers.routes.ClientRegistrationPendingWithOurServiceController.onPageLoad(waypoints, pendingRegistration.journeyId)))
            case None if pendingRegistrations.nonEmpty =>
              Some(Redirect(controllers.routes.ClientRegistrationPendingWithAnotherIntermediaryController.onPageLoad()))
            case None =>
              None
          }
        case Left(_) => None
      }
  }
}
