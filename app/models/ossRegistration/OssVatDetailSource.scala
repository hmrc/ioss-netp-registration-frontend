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

package models.ossRegistration

import models.{Enumerable, WithName}

sealed trait OssVatDetailSource

object OssVatDetailSource extends Enumerable.Implicits {
  case object Etmp        extends WithName("etmp") with OssVatDetailSource
  case object UserEntered extends WithName("userEntered") with OssVatDetailSource
  case object Mixed       extends WithName("mixed") with OssVatDetailSource

  val values: Seq[OssVatDetailSource] = Seq(
    Etmp, UserEntered, Mixed
  )

  implicit val enumerable: Enumerable[OssVatDetailSource] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
