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

package controllers.actions

import models.requests.{DataRequest, OptionalDataRequest}
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder, MessagesActionBuilder, MessagesControllerComponents, PlayBodyParsers}
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuthenticatedControllerComponents extends MessagesControllerComponents {
  
  def actionBuilder: DefaultActionBuilder
  
  def sessionRepository: SessionRepository
  
  def identify: IdentifierAction
  
  def getData: DataRetrievalAction
  
  def requireData: DataRequiredAction

  def identifyAndGetData: ActionBuilder[DataRequest, AnyContent] =
    actionBuilder andThen
      identify andThen
      getData andThen
      requireData

  def identifyAndGetOptionalData: ActionBuilder[OptionalDataRequest, AnyContent] =
    actionBuilder andThen
      identify andThen
      getData
}

case class DefaultAuthenticatedControllerComponents @Inject()(
                                                                 messagesActionBuilder: MessagesActionBuilder,
                                                                 actionBuilder: DefaultActionBuilder,
                                                                 parsers: PlayBodyParsers,
                                                                 messagesApi: MessagesApi,
                                                                 langs: Langs,
                                                                 fileMimeTypes: FileMimeTypes,
                                                                 executionContext: ExecutionContext,
                                                                 sessionRepository: SessionRepository,
                                                                 identify: IdentifierAction,
                                                                 getData: DataRetrievalAction,
                                                                 requireData: DataRequiredAction
                                                               ) extends AuthenticatedControllerComponents
