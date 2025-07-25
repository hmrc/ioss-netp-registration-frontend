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

package controllers

import logging.Logging
import models.UserAnswers
import models.requests.DataRequest
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.libs.json.{JsArray, JsObject, Reads}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.{Derivable, Gettable, Settable}
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future
import scala.util.Try

trait AnswerExtractor extends Logging {

  def getAnswer[A](waypoints: Waypoints, query: Gettable[A])
                  (block: A => Result)
                  (implicit request: DataRequest[AnyContent], ev: Reads[A]): Result = {
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse({
        logAnswerNotFoundMessage(query)
        Redirect(JourneyRecoveryPage.route(waypoints).url)
      })
  }

  def getAnswerAsync[A](waypoints: Waypoints, query: Gettable[A])
                       (block: A => Future[Result])
                       (implicit request: DataRequest[AnyContent], ev: Reads[A]): Future[Result] = {
    request.userAnswers
      .get(query)
      .map(block(_))
      .getOrElse({
        logAnswerNotFoundMessage(query)
        Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
      })
  }

  def cleanupEmptyAnswers(answers: UserAnswers, derivable: Derivable[Seq[JsObject], Int], query: Settable[JsArray]): Try[UserAnswers] = {
    answers.get(derivable) match {
      case Some(n) if n == 0 => answers.remove(query)
      case _ => Try(answers)
    }
  }

  private def logAnswerNotFoundMessage[T](query: Gettable[T]): Unit = logger.warn(s"$query question has not been answered")

}