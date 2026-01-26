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

package controllers.actions

import models.requests.{AuthenticatedMandatoryRegistrationRequest, DataRequest, OptionalDataRequest}
import pages.Page
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuthenticatedControllerComponents extends MessagesControllerComponents {

  def actionBuilder: DefaultActionBuilder

  def sessionRepository: SessionRepository

  def identify: IdentifierAction

  def getData: DataRetrievalAction

  def clientIdentify: ClientIdentifierAction

  def clientGetData: ClientDataRetrievalAction

  def requireData: DataRequiredAction

  def limitIndex: MaximumIndexFilterProvider
  
  def netpValidation: NetpValidationFilterProvider

  def requireRegistration: RegistrationRequiredAction

  def checkAmendAccess: CheckAmendPageAccessFilter

  def checkExcludedIntermediary: CheckExcludedIntermediaryFilterProvider

  def checkIntermediaryAccess: CheckIntermediaryAccessFilterProvider

  def identifyAndGetData(inAmend: Boolean = false, checkAmendAccess: Option[Page] = None): ActionBuilder[DataRequest, AnyContent] = {
    val baseActions = identifyAndGetOptionalData(inAmend, checkAmendAccess) andThen
      requireData(inAmend)

      (inAmend, checkAmendAccess) match {
      case (true, Some(page)) => baseActions andThen this.checkAmendAccess(page)
      case _ => baseActions
    }
  }

  def identifyAndGetOptionalData(
                                  inAmend: Boolean = false,
                                  checkAmendAccess: Option[Page] = None,
                                  iossNumber: Option[String] = None
                                ): ActionBuilder[OptionalDataRequest, AnyContent] = {
    val baseActions = actionBuilder andThen
      identify andThen
      getData andThen
      checkExcludedIntermediary() andThen
      checkIntermediaryAccess(iossNumber)

    (inAmend, checkAmendAccess) match {
      case (true, Some(page)) => baseActions andThen this.checkAmendAccess.forOptionalData(page)
      case _ => baseActions
    }
  }

  def identifyAndRequireRegistration(
                                      inAmend: Boolean
                                    ): ActionBuilder[AuthenticatedMandatoryRegistrationRequest, AnyContent] = {
    identifyAndGetData(inAmend) andThen
      requireRegistration()
  }
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
                                                               requireData: DataRequiredAction,
                                                               limitIndex: MaximumIndexFilterProvider,
                                                               netpValidation: NetpValidationFilterProvider,
                                                               clientIdentify: ClientIdentifierAction,
                                                               clientGetData: ClientDataRetrievalAction,
                                                               requireRegistration: RegistrationRequiredAction,
                                                               checkAmendAccess: CheckAmendPageAccessFilter,
                                                               checkExcludedIntermediary: CheckExcludedIntermediaryFilterProvider,
                                                               checkIntermediaryAccess: CheckIntermediaryAccessFilterProvider
                                                             ) extends AuthenticatedControllerComponents
