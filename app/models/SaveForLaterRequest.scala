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

package models

import models.domain.VatCustomerInfo
import models.etmp.EtmpIdType
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.domain.Vrn

case class SaveForLaterRequest(
                                journeyID: String,
                                data: JsValue,
                                intermediaryNumber: String,
                              )

object SaveForLaterRequest {

  implicit val format: OFormat[SaveForLaterRequest] = Json.format[SaveForLaterRequest]

  def apply(userAnswers: UserAnswers, intermediaryNumber: String): SaveForLaterRequest = {
    
    SaveForLaterRequest(journeyID = userAnswers.journeyId, data = userAnswers.data, intermediaryNumber = intermediaryNumber)
  }
}




/** Notes below on the change of this class 
 TLDR -> Not all NETP have VRN so makes no sense to use as their ID, we perform no checks on ID's we cannot check i.e. FTR, All have a uniqueID and Intermediary Number which is needed to call the DB later on*/


/**
Original case class:
 case class SaveForLaterRequest(
 vrn: Vrn,
 data: JsValue,
 vatInfo: Option[VatCustomerInfo]
 )

It appears we send the VRN as a way to identify this 
 // Why are we sending of the VRN? is this to determine the intermediary? could we do this with intermediaryNumber??
 // After checking the BE it appears we just use this to identify the answers? 
 // Intermediary Number should always be present? 
 // But could the Intermediary start the journey for one, then to another? 
 // The link in the dashboard needs the tax identifier number so i believe this will need to be stored in the data request. 
 
 
Option 1: 
 We could store things by JourneyID- This would make sure they are unique. When reading from the database run a similar method to: getCustomerIdentification to get the data. 
 Pro- always unique
 Con- More logic when reading, possibly more logic when generating next page
 
 Option 2: 
 Store things by their customerIdentification i.e. VRN etc. We never ensure these are unique
 */ 