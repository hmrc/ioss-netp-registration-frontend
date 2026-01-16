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

package models.securemessage

import play.api.libs.json.*

case class SecureMessageResponse(
                              messageType: String,
                              id: String,
                              subject: String,
                              issueDate: String,
                              senderName: String,
                              unreadMessages: Boolean,
                              count: Long,
                              taxpayerName: Option[TaxpayerName] = None,
                              validFrom: String,
                              sentInError: Boolean,
                              language: Option[String]
                            )

object SecureMessageResponse {

  val reads: Reads[SecureMessageResponse] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "messageType").read[String] and
      (__ \ "id").read[String] and
      (__ \ "subject").read[String] and
      (__ \ "issueDate").read[String] and
      (__ \ "senderName").read[String] and
      (__ \ "unreadMessages").read[Boolean] and
      (__ \ "count").read[Long] and
      (__ \ "taxpayerName").readNullable[TaxpayerName] and
      (__ \ "validFrom").read[String] and
      (__ \ "sentInError").read[Boolean] and
      (__ \ "language").readNullable[String]
    )(SecureMessageResponse.apply _)
  }

  val writes: OWrites[SecureMessageResponse] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "messageType").write[String] and
        (__ \ "id").write[String] and
        (__ \ "subject").write[String] and
        (__ \ "issueDate").write[String] and
        (__ \ "senderName").write[String] and
        (__ \ "unreadMessages").write[Boolean] and
        (__ \ "count").write[Long] and
        (__ \ "taxpayerName").writeNullable[TaxpayerName] and
        (__ \ "validFrom").write[String] and
        (__ \ "sentInError").write[Boolean] and
        (__ \ "language").writeNullable[String]
    )(secureMessageResponse => Tuple.fromProductTyped(secureMessageResponse))
  }

  implicit val formats: OFormat[SecureMessageResponse] = OFormat(reads, writes)
}
