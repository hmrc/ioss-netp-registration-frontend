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

package pages.website

import models.{Index, UserAnswers}
import pages.{Page, Waypoints}
import play.api.mvc.Call
import queries.DeriveNumberOfWebsites

case class DeleteWebsitePage(index: Index) extends Page {

  override def route(waypoints: Waypoints): Call =
    controllers.website.routes.DeleteWebsiteController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    navigate(answers)

  private def navigate(answers: UserAnswers): Page = {
    answers.get(DeriveNumberOfWebsites) match {
      case Some(n) if n > 0 => AddWebsitePage()
      case _ => WebsitePage(Index(0))
    }
  }

}
