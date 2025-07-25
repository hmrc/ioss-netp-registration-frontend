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

package models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn

case class IdentifierRequest[A] (
                                  request: Request[A],
                                  userId: String,
                                  enrolments: Enrolments,
                                  vrn: Vrn,
                                  intermediaryNumber: String
                                ) extends WrappedRequest[A](request)

case class SessionRequest[A](request: Request[A], userId: String) extends WrappedRequest[A](request)