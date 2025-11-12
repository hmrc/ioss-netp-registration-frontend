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

import logging.Logging
import models.UserAnswers
import models.requests.{DataRequest, OptionalDataRequest}
import pages.*
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckAmendPageAccessFilter @Inject()(
                                            implicit val executionContext: ExecutionContext
                                          ) extends Logging {

  private[actions] def shouldBlockPage(maybeUserAnswers: Option[UserAnswers], page: QuestionPage[_]): Boolean = {
    maybeUserAnswers.exists { userAnswers =>
      val blockedPages = getBlockedPages(userAnswers)
      blockedPages.contains(page)
    }
  }

  private[actions] def getBlockedPages(userAnswers: UserAnswers): Seq[QuestionPage[_]] = {
    val isUkBased = userAnswers.get(BusinessBasedInUKPage).getOrElse(true)
    val hasVatNumber = userAnswers.get(ClientHasVatNumberPage).getOrElse(false)
    val hasUtrNumber = userAnswers.get(ClientHasUtrNumberPage).getOrElse(false)

    val alwaysBlocked = Seq(BusinessBasedInUKPage, ClientHasVatNumberPage, CheckVatDetailsPage())

    val additionalBlocked = (isUkBased, hasVatNumber, hasUtrNumber) match {
      case (true, true, _) => Seq(ClientVatNumberPage, ClientBusinessAddressPage)
      case (true, false, true) => Seq(ClientHasUtrNumberPage, ClientUtrNumberPage)
      case (true, false, false) => Seq(ClientHasUtrNumberPage, ClientsNinoNumberPage)
      case (false, true, _) => Seq(ClientVatNumberPage)
      case (false, false, _) => Seq.empty
    }

    alwaysBlocked ++ additionalBlocked
  }

  def apply(page: QuestionPage[_]): ActionFilter[DataRequest] = new ActionFilter[DataRequest] {
    override protected def executionContext: ExecutionContext = CheckAmendPageAccessFilter.this.executionContext

    override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
      if (shouldBlockPage(Some(request.userAnswers), page)) {
        logger.info(s"Blocked access to ${page.toString}")
        Future.successful(Some(Redirect(controllers.amend.routes.ChangeRegistrationController.onPageLoad())))
      } else {
        Future.successful(None)
      }
    }
  }

  def forOptionalData(page: QuestionPage[_]): ActionFilter[OptionalDataRequest] = new ActionFilter[OptionalDataRequest] {
    override protected def executionContext: ExecutionContext = CheckAmendPageAccessFilter.this.executionContext

    override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
      if (shouldBlockPage(request.userAnswers, page)) {
        logger.info(s"Blocked access to ${page.toString}")
        Future.successful(Some(Redirect(controllers.amend.routes.ChangeRegistrationController.onPageLoad())))
      } else {
        Future.successful(None)
      }
    }
  }
}