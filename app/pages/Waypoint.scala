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

package pages

import models.{CheckMode, Mode, NormalMode}
import pages.previousRegistrations.{AddPreviousRegistrationPage, CheckPreviousSchemeAnswersPage}
import pages.tradingNames.AddTradingNamePage
import pages.vatEuDetails.{AddEuDetailsPage, CheckEuDetailsAnswersPage}
import pages.website.AddWebsitePage

import scala.language.postfixOps

case class Waypoint(
                     page: WaypointPage,
                     mode: Mode,
                     urlFragment: String
                   )

object Waypoint {

  private val fragments: Map[String, Waypoint] =
    Map(
      AddTradingNamePage().normalModeUrlFragment -> AddTradingNamePage().waypoint(NormalMode),
      AddTradingNamePage().checkModeUrlFragment -> AddTradingNamePage().waypoint(CheckMode),
      AddPreviousRegistrationPage().normalModeUrlFragment -> AddPreviousRegistrationPage().waypoint(NormalMode),
      AddPreviousRegistrationPage().checkModeUrlFragment -> AddPreviousRegistrationPage().waypoint(CheckMode),
      AddWebsitePage().normalModeUrlFragment ->  AddWebsitePage().waypoint(NormalMode),
      AddWebsitePage().checkModeUrlFragment ->  AddWebsitePage().waypoint(CheckMode),
      AddEuDetailsPage().normalModeUrlFragment -> AddEuDetailsPage().waypoint(NormalMode),
      AddEuDetailsPage().checkModeUrlFragment -> AddEuDetailsPage().waypoint(CheckMode),
      CheckVatDetailsPage().urlFragment -> CheckVatDetailsPage().waypoint,
      CheckYourAnswersPage.urlFragment -> CheckYourAnswersPage.waypoint
    )

  def fromString(s: String): Option[Waypoint] =
    fragments.get(s)
      .orElse(CheckPreviousSchemeAnswersPage.waypointFromString(s))
      .orElse(CheckEuDetailsAnswersPage.waypointFromString(s))
}
