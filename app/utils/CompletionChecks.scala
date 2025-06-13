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

package utils

import models.Index
import models.requests.DataRequest
import pages.Waypoints
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.AllWebsites

import scala.concurrent.Future

trait CompletionChecks {

  protected def withCompleteDataModel[A](index: Index, data: Index => Option[A], onFailure: Option[A] => Result)
                                        (onSuccess: => Result): Result = {
    val incomplete = data(index)
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {
    val incomplete = data()
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  def validate()(implicit request: DataRequest[AnyContent]): Boolean = {
    hasWebsiteValid()
  }

  def getFirstValidationErrorRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    (incompleteWebsiteUrlsRedirect(waypoints)).headOption
  }

  private def hasWebsiteValid()(implicit request: DataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(AllWebsites).getOrElse(List.empty).nonEmpty
  }

  private def incompleteWebsiteUrlsRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!hasWebsiteValid()) {
    Some(Redirect(controllers.website.routes.WebsiteController.onPageLoad(waypoints, Index(0))))
  } else {
    None
  }
}
