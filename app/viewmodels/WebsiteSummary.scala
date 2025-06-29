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

package viewmodels

import models.{Index, UserAnswers}
import pages.Waypoints
import pages.website.{AddWebsitePage, DeleteWebsitePage, WebsitePage}
import play.twirl.api.HtmlFormat
import queries.AllWebsites

object WebsiteSummary  {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, sourcePage: AddWebsitePage): Seq[ListItemWrapper] =
    answers.get(AllWebsites).getOrElse(List.empty).zipWithIndex.map {
      case (website, index) =>
      ListItemWrapper(
        ListItem(
          name = HtmlFormat.escape(website.site).toString,
          changeUrl = WebsitePage(Index(index)).changeLink(waypoints, sourcePage).url,
          removeUrl = DeleteWebsitePage(Index(index)).route(waypoints).url
        ),
        removeButtonEnabled = true
      )
    }
}
