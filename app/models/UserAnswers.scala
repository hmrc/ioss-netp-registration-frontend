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
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import queries.{Derivable, Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.UUID
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
                              id: String,
                              journeyId: String = UUID.randomUUID().toString,
                              data: JsObject = Json.obj(),
                              vatInfo: Option[VatCustomerInfo] = None,
                              lastUpdated: Instant = Instant.now
                            ) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def get[A, B](derivable: Derivable[A, B])(implicit rds: Reads[A]): Option[B] = {
    Reads.optionNoError(Reads.at(derivable.path))
      .reads(data)
      .getOrElse(None)
      .map(derivable.derive)
  }

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy(data = d)
        page.cleanup(Some(value), updatedAnswers)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.flatMap {
      d =>
        val updatedAnswers = copy(data = d)
        page.cleanup(None, updatedAnswers)
    }
  }

  def isDefined(gettable: Gettable[_]): Boolean =
    Reads.optionNoError(Reads.at[JsValue](gettable.path)).reads(data)
      .map(_.isDefined)
      .getOrElse(false)
}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    (
      (__ \ "_id").read[String] and
        (__ \ "journeyId").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "vatInfo").readNullable[VatCustomerInfo] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    (
      (__ \ "_id").write[String] and
        (__ \ "journeyId").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "vatInfo").writeNullable[VatCustomerInfo] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(userAnswers => Tuple.fromProductTyped(userAnswers))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
