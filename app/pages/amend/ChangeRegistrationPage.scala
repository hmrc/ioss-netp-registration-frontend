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

package pages.amend

import pages.{CheckAnswersPage, Page, Waypoint, Waypoints}
import play.api.mvc.Call

case class ChangeRegistrationPage(iossNum: String) extends CheckAnswersPage {

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: ChangeRegistrationPage => p.iossNum == this.iossNum
    case _ => false
  }

  override val urlFragment: String = s"change-your-registration-$iossNum"

  override def route(waypoints: Waypoints): Call =
    controllers.amend.routes.ChangeRegistrationController.onPageLoad(waypoints, iossNum)

}

object ChangeRegistrationPage {
  def waypointFromString(s: String): Option[Waypoint] ={
    val pattern = """change-your-registration-(.+)""".r.anchored

    s match {
      case pattern(iossNum) =>
        Some(ChangeRegistrationPage(iossNum).waypoint)
      case _ => None
    }
  }
}