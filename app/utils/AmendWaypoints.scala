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

import pages.amend.ChangeRegistrationPage
import pages.{CheckAnswersPage, CheckYourAnswersPage, NonEmptyWaypoints, Waypoints}

object AmendWaypoints {
  implicit class AmendWaypointsOps(waypoints: Waypoints) {

    private def isInMode(pages: CheckAnswersPage*): Boolean = {
      waypoints match
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          pages.exists(pages => nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(ChangeRegistrationPage.urlFragment))

        case _ =>
          false
    }

    def inCheck: Boolean = {
      isInMode(CheckYourAnswersPage)
    }

    def inAmend: Boolean = {
      isInMode(ChangeRegistrationPage)
    }

    def getNextCheckYourAnswersPageFromWaypoints: Option[CheckAnswersPage] = {
      waypoints match {
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          List(ChangeRegistrationPage, CheckYourAnswersPage).find { page =>
            nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(page.urlFragment)
          }

        case _ =>
          None
      }
    }
  }
}
